# Item Filter & Search System - Documentation

## 📋 Mục lục
1. [Tổng quan](#tổng-quan)
2. [Luồng hoạt động](#luồng-hoạt-động)
3. [API Endpoints](#api-endpoints)
4. [Search System](#search-system)
5. [Filter Parameters Detail](#filter-parameters-detail)
6. [Database Schema](#database-schema)
7. [Pagination & Sorting](#pagination--sorting)
8. [Response DTOs](#response-dtos)
9. [File Structure](#file-structure)
10. [Mapper Architecture](#mapper-architecture)

---

## Tổng quan

Hệ thống cung cấp 2 bộ API riêng biệt cho customer-facing:

### 1. Item Filter API (`/item-filter`)
- **Purpose**: Filter chi tiết theo nhiều tiêu chí
- **Features**:
  - Filter đa dạng theo từng entity
  - Range filters (min/max)
  - Date range filters
  - Hardcoded `isApproved = true`

### 2. Item Search API (`/item-search`)
- **Purpose**: Tìm kiếm theo keyword với fuzzy matching
- **Features**:
  - Case-insensitive search với `LOWER()` và `LIKE`
  - Fuzzy re-ranking với Vietnamese accent handling (Java-side)
  - Jaro-Winkler + Levenshtein similarity algorithms
  - Re-ranking results by relevance score (khi sortBy=relevance)
  - Sort by relevance hoặc các trường khác (không override user sort)
  - Pagination chính xác

### Entities được hỗ trợ
- **Accommodation** - Lọc/tìm nơi cư trú
- **Tour** - Lọc/tìm tour du lịch
- **TransportTicket** - Lọc/tìm vé di chuyển
- **Voucher** - Lọc/tìm voucher giảm giá

### Đặc điểm chung
- Tách riêng hoàn toàn với logic cũ (clean architecture)
- Pagination với page size limit = 100
- Tránh N+1 query bằng batch fetching và JOIN FETCH
- **Chỉ hiển thị items đã duyệt** (`isApproved = true` hardcoded)
- Không expose thông tin nhạy cảm (voucher codes, approval status)

---

## Luồng hoạt động

### 🔄 Flow Chung - Filter

```
CLIENT REQUEST
      │
      ▼
┌─────────────────────────────────────────────────────────────────────┐
│  ItemFilterController                                               │
│  - Parse request params                                              │
│  - Validate params                                                   │
│  - Call service                                                      │
└─────────────────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────────────┐
│  ItemFilterService                                                  │
│  - Build filter criteria                                            │
│  - Call repository (single query)                                   │
│  - Batch fetch images từ MongoDB                                     │
│  - Map to response DTOs                                             │
└─────────────────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────────────┐
│  FilterRepository (Jpa)                                             │
│  - Dynamic WHERE clause (predicates)                                 │
│  - JOIN FETCH for related entities                                   │
│  - Pageable (pagination + sort)                                      │
└─────────────────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────────────┐
│  MongoDB Images Repository                                          │
│  - Batch fetch images by refId (UUIDs)                              │
│  - Single query cho multiple items                                   │
└─────────────────────────────────────────────────────────────────────┘
      │
      ▼
RESPONSE
```

### 🔄 Flow Chung - Search với Fuzzy Matching

```
CLIENT REQUEST: GET /item-search/accommodations?keyword=Hà Nội
      │
      ▼
┌─────────────────────────────────────────────────────────────────────┐
│  ItemSearchController                                               │
│  - Parse keyword + pagination                                        │
│  - Call service                                                      │
└─────────────────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────────────┐
│  ItemSearchService                                                  │
│  1. Call search repository (case-insensitive LIKE)                  │
│  2. Batch fetch images from MongoDB                                 │
│  3. Re-rank results with FuzzySearchService (if sortBy=relevance)  │
│     - Items match exact keyword: HIGH score                         │
│     - Items match with accents (Hà Nội): LOWER score but KEEP      │
│     - Items not matching: LOWEST score but KEEP                     │
└─────────────────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────────────┐
│  SearchRepository (Jpa)                                             │
│  WHERE is_approved = true                                           │
│    AND LOWER(name) LIKE '%hà nội%'                                  │
│    AND LOWER(city) LIKE '%hà nội%'                                  │
│  NOTE: Case-insensitive only, diacritics preserved                  │
└─────────────────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────────────┐
│  FuzzySearchService.reRankByFuzzyScore()                            │
│  - Exact match: +100 points                                         │
│  - Unaccent match (Hà Nội → Ha Noi): +70 points                    │
│  - Similarity > 0.5: +50 points × similarity                       │
│  - Sort by score DESC, then by createdAt DESC                       │
│  IMPORTANT: NO items filtered out - all items kept but re-ranked     │
└─────────────────────────────────────────────────────────────────────┘
      │
      ▼
RESPONSE (paginated, re-ranked by relevance)
```

---

## API Endpoints

### Filter API - Base URL: `/item-filter`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/accommodations` | Filter accommodations |
| GET | `/tours` | Filter tours |
| GET | `/transport-tickets` | Filter transport tickets |
| GET | `/vouchers` | Filter vouchers |

### Search API - Base URL: `/item-search`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/accommodations` | Search accommodations by keyword |
| GET | `/tours` | Search tours by keyword |
| GET | `/transport-tickets` | Search transport tickets by keyword |
| GET | `/vouchers` | Search vouchers by keyword |

---

## Search System

### Search Implementation
- **Search Method**: LIKE query với `LOWER()` cho case-insensitive matching
- **Vietnamese Accent Handling**: Java-side normalization trong `FuzzySearchService`
- **Hybrid Approach**: 
  - DB query: Case-insensitive LIKE (không xử lý accent)
  - Java re-ranking: Accent-insensitive filtering + fuzzy scoring
- **Key Benefit**: Pagination chính xác - không có items bị loại bỏ

### Vietnamese Search Implementation

**Database Level (Case-Insensitive Only):**
```java
// JPA Criteria API - basic LOWER() LIKE
cb.like(cb.lower(root.get("city")), "%" + keyword.toLowerCase() + "%")

// SQL Generated:
// WHERE LOWER(city) LIKE '%hà nội%'

// Ví dụ:
// Database value: "Hà Nội"
// LOWER("Hà Nội") = "hà nội"  (diacritics preserved)
// LIKE '%hà nội%' → MATCH ✓ (case-insensitive)
// LIKE '%ha noi%' → NO MATCH (diacritics different)
```

**Java Level (Accent-Insensitive Re-ranking):**
```java
// FuzzySearchService.reRankByFuzzyScore()
// 1. Remove accents từ cả keyword và field values
// 2. Tính similarity score
// 3. Re-rank items theo score

// Ví dụ:
// "Hà Nội Hotel" với keyword "ha noi":
// - removeAccents("ha noi") = "ha noi"
// - removeAccents("Hà Nội Hotel") = "Ha Noi Hotel"
// - similarity("ha noi", "Ha Noi Hotel") ≈ 0.6 → score = 30
// Item vẫn ĐƯỢC GIỮ trong results nhưng xếp hạng THẤP
```

### IMPORTANT: No Items Filtered Out

**Điểm quan trọng của hybrid approach:**

```
Search "ha noi" (case-insensitive, diacritics preserved):
├── Items match "ha noi" → HIGH rank
├── Items match "Hà Nội" → LOWER rank nhưng VẪN CÓ
└── Items don't match → LOWEST rank nhưng VẪN CÓ

→ Pagination CHÍNH XÁC: totalElements = số items từ DB
→ Không có empty pages do filter
```

### Search Fields theo Entity

#### Accommodation Search
| Field | Description |
|-------|-------------|
| `accommodationName` | Tên khách sạn |
| `city` | Thành phố |
| `accommodationType` | Loại: hotel, resort, villa... |
| `province` | Tỉnh/thành phố |

#### Tour Search
| Field | Description |
|-------|-------------|
| `tourName` | Tên tour |
| `dropOffPoint` | Điểm đến |
| `meetingPoint` | Điểm khởi hành |

#### TransportTicket Search
| Field | Description |
|-------|-------------|
| `providerName` | Hãng vận tải |
| `departureLocation` | Điểm đi |
| `arrivalLocation` | Điểm đến |
| `ticketType` | Loại phương tiện |

#### Voucher Search
| Field | Description |
|-------|-------------|
| `nameVoucher` | Tên voucher |
| `sponsorType` | SYSTEM/BUSINESS |
| `applyTo` | ALL, ROOM, TOUR... |

### Fuzzy Search Scoring System

| Match Type | Score | Description |
|------------|-------|-------------|
| **Exact Match** | 100 | Case-insensitive exact match: `hotel` matches `Hotel` |
| **Unaccent Match** | 70 | Vietnamese diacritics removed: `Ha Noi` matches `Hà Nội` |
| **Similarity Match** | 50 × similarity | Jaro-Winkler + Levenshtein > 0.5 threshold |

### FuzzySearchService Methods

```java
@Service
public class FuzzySearchService {

    // 1. Remove Vietnamese diacritics
    // "Hà Nội" → "Ha Noi"
    public String removeAccents(String input)

    // 2. Calculate similarity (Jaro-Winkler + Levenshtein)
    // Returns score 0.0 - 1.0
    public double calculateSimilarity(String s1, String s2)

    // 3. Re-rank results by fuzzy score (NO filtering)
    // Used for Search API - keeps all items but reorders by relevance
    public <T> List<T> reRankByFuzzyScore(
        List<T> results,
        String keyword,
        Function<T, String>... textExtractors
    )

    // 4. Filter AND re-rank results (filters out non-matching items)
    // Used for Filter API - removes items that don't match keyword
    public <T> List<T> filterAndReRankByFuzzyScore(
        List<T> results,
        String keyword,
        Function<T, String>... textExtractors
    )
}
```

### When to Use Which Method

| Method | Use Case | Effect on Results |
|--------|----------|-------------------|
| `reRankByFuzzyScore` | Search API | All items KEPT, only reordered |
| `filterAndReRankByFuzzyScore` | Filter API (if needed) | Non-matching items REMOVED |

### Search Example

**User searches:** `Ha Noi`

**Results (before re-ranking):**
- "Hà Nội Hotel" (city contains "hà nội" - case-insensitive match)
- "Grand Hotel Sài Gòn" (contains "hotel" - case-insensitive match)
- "Rex Hotel Sài Gòn" (contains "hotel" - case-insensitive match)
- "Hanoi Emerald Hotel" (contains "hanoi" - case-insensitive match)

**Results (after fuzzy re-ranking with `reRankByFuzzyScore`):**
1. "Hà Nội Hotel" - score: 70 (unaccent match: "ha noi" ≈ "ha noi")
2. "Hanoi Emerald Hotel" - score: 70 (unaccent match: "ha noi" ≈ "hanoi")  
3. "Grand Hotel Sài Gòn" - score: ~30 (similarity match: "ha noi" vs "hotel")
4. "Rex Hotel Sài Gòn" - score: ~30 (similarity match)

**All 4 items are KEPT** - only the order changes based on relevance score.

---

## Filter Parameters Detail

### Accommodation Filter (`/item-filter/accommodations`)

| Param | Type | Description |
|-------|------|-------------|
| `keyword` | String | Tìm kiếm tên, mô tả, địa chỉ |
| `accommodationType` | String | hotel, hostel, resort, villa, apartment, homestay, motel |
| `province` | String | Tỉnh/thành |
| `city` | String | Quận/huyện |
| `ward` | String | Phường/xã |
| `minPrice` | BigDecimal | Giá min |
| `maxPrice` | BigDecimal | Giá max |
| `sortBy` | String | accommodationName, city, province, price... |
| `sortDir` | String | ASC/DESC |
| `page` | int | Trang (0-indexed) |
| `size` | int | Kích thước trang (max 100) |

### Tour Filter (`/item-filter/tours`)

| Param | Type | Description |
|-------|------|-------------|
| `keyword` | String | Tìm kiếm tên tour, mô tả, điểm đến |
| `minAdultPrice` | BigDecimal | Giá người lớn min |
| `maxAdultPrice` | BigDecimal | Giá người lớn max |
| `minChildPrice` | BigDecimal | Giá trẻ em min |
| `maxChildPrice` | BigDecimal | Giá trẻ em max |
| `minDuration` | Integer | Thời lượng min (phút) |
| `maxDuration` | Integer | Thời lượng max |
| `durationType` | String | HALF_DAY, FULL_DAY, MULTIPLE_DAYS |
| `languages` | String | Ngôn ngữ hướng dẫn |
| `departureLocation` | String | Điểm khởi hành |
| `destination` | String | Điểm đến |
| `tourType` | String | ADVENTURE, CULTURAL, BEACH, MOUNTAIN, CITY_TOUR... |
| `difficulty` | String | EASY, MODERATE, CHALLENGING |
| `sortBy` | String | tourName, adultPrice, durationMinutes... |
| `sortDir` | String | ASC/DESC |
| `page` | int | Trang |
| `size` | int | Kích thước trang |

### TransportTicket Filter (`/item-filter/transport-tickets`)

| Param | Type | Description |
|-------|------|-------------|
| `keyword` | String | Tìm kiếm nhà cung cấp, loại vé |
| `ticketType` | String | PLANE, BUS, TRAIN, FERRY, CAR_RENTAL |
| `ticketClass` | String | ECONOMY, BUSINESS, FIRST, PREMIUM_ECONOMY |
| `providerName` | String | Tên hãng vận tải |
| `departureLocation` | String | Điểm đi |
| `arrivalLocation` | String | Điểm đến |
| `departureFrom` | LocalDateTime | Giờ khởi hành từ |
| `departureTo` | LocalDateTime | Giờ khởi hành đến |
| `arrivalFrom` | LocalDateTime | Giờ đến từ |
| `arrivalTo` | LocalDateTime | Giờ đến đến |
| `minPrice` | BigDecimal | Giá min |
| `maxPrice` | BigDecimal | Giá max |
| `minAvailableSlots` | Integer | Số chỗ trống tối thiểu |
| `sortBy` | String | providerName, price, departureTime... |
| `sortDir` | String | ASC/DESC |
| `page` | int | Trang |
| `size` | int | Kích thước trang |

### Voucher Filter (`/item-filter/vouchers`)

| Param | Type | Description |
|-------|------|-------------|
| `keyword` | String | Tìm kiếm tên voucher |
| `code` | String | Mã voucher chính xác |
| `minDiscountPercent` | BigDecimal | % giảm min |
| `maxDiscountPercent` | BigDecimal | % giảm max |
| `minDiscountAmount` | BigDecimal | Số tiền giảm min |
| `maxDiscountAmount` | BigDecimal | Số tiền giảm max |
| `sponsorType` | String | SYSTEM, BUSINESS |
| `applyTo` | String | ALL, ROOM, SERVICE, TOUR, TICKET |
| `validFrom` | LocalDate | Có hiệu lực từ ngày |
| `validTo` | LocalDate | Có hiệu lực đến ngày |
| `minQuantity` | Integer | Số lượng còn lại tối thiểu |
| `accommodationId` | UUID | Voucher của accommodation cụ thể |
| `expireSoon` | Boolean | Hết hạn trong 7 ngày |
| `newVoucher` | Boolean | Voucher mới (trong 7 ngày) |
| `sortBy` | String | nameVoucher, discountPercent, endDate... |
| `sortDir` | String | ASC/DESC |
| `page` | int | Trang |
| `size` | int | Kích thước trang |

### Search Parameters (All Entities)

| Param | Type | Description |
|-------|------|-------------|
| `keyword` | String | Từ khóa tìm kiếm (case-insensitive ở DB, accent-handling ở Java re-ranking) |
| `sortBy` | String | relevance (default), createdAt, name... |
| `sortDir` | String | ASC/DESC |
| `page` | int | Trang (0-indexed) |
| `size` | int | Kích thước trang (max 100) |

**Note:** Khi `sortBy=relevance`, results sẽ được re-rank bằng fuzzy score. Khi `sortBy` khác (vd: `price`), fuzzy re-ranking KHÔNG được áp dụng - kết quả giữ nguyên sort từ database.

---

## Database Schema

### Accommodation → Images (MongoDB)
```
PostgreSQL: accommodation
  └── id_accommodation (UUID, PK)

MongoDB: images
  └── refId (String) → link to accommodation.id_accommodation
```

### Tour → TourSchedule
```
PostgreSQL: tour
  └── id_tour (UUID, PK)

PostgreSQL: tour_schedule
  └── id_schedule (UUID, PK)
  └── id_tour (FK) → tour.id_tour
```

### Voucher → Accommodation → Users
```
PostgreSQL: voucher
  └── id_voucher (UUID, PK)
  └── id_accommodation (FK) → accommodation.id_accommodation

PostgreSQL: accommodation
  └── id_accommodation (UUID, PK)
  └── id_user (FK) → users.id_user
```

---

## Pagination & Sorting

### Default Values
| Param | Default | Max |
|-------|---------|-----|
| `page` | 0 | - |
| `size` | 20 | 100 |
| `sortDir` | DESC | - |

### Sort Fields - Filter

#### Accommodation
- `accommodationName` (default), `city`, `province`, `createdAt`, `starRating`, `guestRating`, `price`, `discountPercent`

#### Tour
- `tourName` (default), `adultPrice`, `durationMinutes`, `createdAt`, `rating`, `departureDate`

#### TransportTicket
- `providerName` (default), `price`, `departureTime`, `duration`, `rating`

#### Voucher
- `nameVoucher` (default), `discountPercent`, `startDate`, `endDate`, `quantity`, `createAt`

### Sort Fields - Search
- `relevance` (default) - sorts by fuzzy match score, then createdAt DESC
- Invalid sort fields default to `relevance` automatically

### Response Structure (FilterPageResponse)
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "hasNext": true,
  "hasPrevious": false
}
```

---

## Response DTOs

### AccommodationFilterResponse
```json
{
  "idAccommodation": "uuid",
  "accommodationName": "string",
  "accommodationType": "string",
  "street": "string",
  "ward": "string",
  "city": "string",
  "province": "string",
  "checkinTime": "HH:mm:ss",
  "checkoutTime": "HH:mm:ss",
  "description": "string",
  "imageUrls": ["url1", "url2"]
}
```

### TourFilterResponse
```json
{
  "idTour": "uuid",
  "tourName": "string",
  "description": "string",
  "durationMinutes": 120,
  "meetingPoint": "string",
  "dropOffPoint": "string",
  "adultPrice": 500000.00,
  "childPrice": 250000.00,
  "languages": "Vietnamese,English",
  "inclusions": ["string"],
  "exclusions": ["string"],
  "additionalInfo": ["string"],
  "status": "string",
  "createdAt": "datetime",
  "tourSchedules": [
    {
      "idSchedule": "uuid",
      "departureDate": "2024-01-15",
      "departureTime": "08:00:00",
      "arrivalLocation": "string",
      "description": "string"
    }
  ],
  "imageUrls": ["url1", "url2", "url3"]
}
```

### TransportTicketFilterResponse
```json
{
  "idTicket": "uuid",
  "ticketType": "PLANE",
  "ticketClass": "ECONOMY",
  "providerName": "Vietnam Airlines",
  "departureLocation": "Hồ Chí Minh",
  "arrivalLocation": "Hà Nội",
  "departureTime": "datetime",
  "arrivalTime": "datetime",
  "price": 500000.00,
  "status": "string",
  "createdAt": "datetime"
}
```

### VoucherFilterResponse
```json
{
  "idVoucher": "uuid",
  "nameVoucher": "string",
  "discountPercent": 15.00,
  "minDiscountAmount": 10000.00,
  "maxDiscountAmount": 50000.00,
  "sponsorType": "SYSTEM",
  "applyTo": "ALL",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "quantity": 100,
  "conditions": "string",
  "createAt": "datetime",
  "imageUrl": "string"
}
```

---

## N+1 Query Prevention

### Solution 1: JOIN FETCH

```java
// Tour - batch load tourSchedules trong 1 query
root.fetch("tourSchedules", JoinType.LEFT);

// Voucher - batch load accommodation trong 1 query
Join<Voucher, Accommodation> accommodationJoin = root.join("accommodation", JoinType.LEFT);
accommodationJoin.join("users", JoinType.LEFT);
```

### Solution 2: Batch Query (MongoDB)

```java
// Accommodation/Tour/Voucher - batch fetch images sau khi lấy entities
List<String> refIds = accommodations.stream()
    .map(a -> a.getIdAccommodation().toString())
    .toList();

// 1 lần call duy nhất
List<Images> allImages = imagesRepository.findByRefIdIn(refIds);

// Map theo refId
Map<String, List<Images>> imagesMap = allImages.stream()
    .collect(Collectors.groupingBy(Images::getRefId));
```

### Solution 3: Pageable

```java
Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
Page<Tour> result = tourRepository.findAllWithFilters(request, pageable);
```

### Summary: Số lượng query cho mỗi filter

| Entity | PostgreSQL Queries | MongoDB Queries |
|--------|-------------------|-----------------|
| Accommodation | 1 | 1 (batch images) |
| Tour | 1 (JOIN FETCH schedules) | 1 (batch images) |
| TransportTicket | 1 | 0 |
| Voucher | 1 (JOIN accommodation+users) | 1 (batch images) |

---

## Mapper Architecture

Hệ thống sử dụng **Interface-based Mapper Pattern** để đảm bảo tính linh hoạt và dễ bảo trì.

### Cấu trúc Mapper

```
Mappers/
├── AccommodationFilterMapper.java        (Interface)
├── AccommodationFilterMapperImpl.java  (Implementation @Component)
├── TourFilterMapper.java                (Interface)
├── TourFilterMapperImpl.java            (Implementation @Component)
├── TransportTicketFilterMapper.java      (Interface)
├── TransportTicketFilterMapperImpl.java  (Implementation @Component)
├── VoucherFilterMapper.java              (Interface)
└── VoucherFilterMapperImpl.java          (Implementation @Component)
```

### Interface Pattern

**Interface:**
```java
public interface AccommodationFilterMapper {
    AccommodationFilterResponse toResponse(Accommodation accommodation, List<Images> images);
}
```

**Implementation:**
```java
@Component
public class AccommodationFilterMapperImpl implements AccommodationFilterMapper {
    @Override
    public AccommodationFilterResponse toResponse(Accommodation accommodation, List<Images> images) {
        // mapping logic
    }
}
```

**Service Usage:**
```java
@Service
@RequiredArgsConstructor
public class ItemFilterService {
    private final AccommodationFilterMapper accommodationFilterMapper;
    // Spring injects AccommodationFilterMapperImpl automatically
}
```

### Lợi ích
1. **Loose coupling** - Service phụ thuộc vào interface không phải implementation
2. **Testability** - Dễ dàng mock mapper trong unit test
3. **Flexibility** - Có thể thay đổi implementation mà không ảnh hưởng service
4. **Clean Architecture** - Tách biệt contract (interface) và implementation

---

## Utils

### VietnameseSearchUtils

```java
public final class VietnameseSearchUtils {
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private VietnameseSearchUtils() {}

    // "Hà Nội" → "ha noi"
    public static String normalizeForSearch(String input) {
        if (input == null) return null;
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String removed = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        return removed.toLowerCase().trim();
    }
}
```

**Usage:** Được sử dụng trong `FuzzySearchService` để normalize keyword và field values trước khi so sánh similarity.

**Note:** Không được sử dụng trong SQL query vì PostgreSQL database không có `unaccent` extension.

---

## Implementation Status

### ✅ Production Ready Features
- All 8 API endpoints (4 Filter + 4 Search)
- Pagination and sorting
- Security (public access, CSRF disabled)
- isApproved = true enforcement
- N+1 query prevention (JOIN FETCH + batch MongoDB)
- Interface-based Mapper pattern

### ⚠️ Known Limitations

1. **Vietnamese Accent Search**
   - Database: Case-insensitive LIKE (diacritics preserved)
   - Java: Accent-insensitive re-ranking
   - **Limitation**: "ha noi" sẽ tìm thấy "Hà Nội" nhưng với score thấp hơn exact match
   - **Workaround**: Sử dụng `VietnameseSearchUtils.removeAccents()` ở Java level

2. **Search without sortBy=relevance**
   - Khi user sort theo field cụ thể (vd: `price ASC`), fuzzy re-ranking không được áp dụng
   - **Đây là behavior đúng** - user muốn sort theo price, không phải relevance

3. **Pagination với Fuzzy Filter**
   - Nếu dùng `filterAndReRankByFuzzyScore`: pagination có thể không chính xác
   - **Solution**: Dùng `reRankByFuzzyScore` (chỉ reorder, không filter)

---

## Error Handling

### Validation Errors
| Error | HTTP Status | Response |
|-------|------------|----------|
| `page < 0` | 400 | "Page number must be >= 0" |
| `size > 100` | 400 | "Page size must be <= 100" |
| `size < 1` | 400 | "Page size must be >= 1" |
| `sortBy` invalid | 400 | Defaults to relevance |
| `sortDir` invalid | 400 | Defaults to DESC |

### Empty Result
```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "hasNext": false,
  "hasPrevious": false
}
```

---

## Performance Best Practices

1. **Luôn dùng Pageable** - Không load all data
2. **JOIN FETCH cho relationship cần thiết** - Không trigger additional queries
3. **Batch query MongoDB** - Dùng `findByRefIdIn()` thay vì loop từng item
4. **Index các cột filter thường dùng** - province, city, price, isApproved
5. **Fuzzy search chỉ re-rank trong memory** - Không query thêm database
6. **Hybrid search approach** - DB query + Java re-ranking để đảm bảo pagination chính xác
7. **Java-side accent removal** - Dùng `VietnameseSearchUtils` thay vì PostgreSQL `unaccent` (tránh dependency)

---

## File Structure

```
src/main/java/com/example/java_service/
├── Controllers/
│   ├── ItemFilterController.java      (Filter API - 4 endpoints)
│   └── ItemSearchController.java      (Search API - 4 endpoints)
├── Services/
│   ├── ItemFilterService.java         (Advanced filter logic)
│   ├── ItemSearchService.java         (Keyword search logic)
│   └── FuzzySearchService.java        (Fuzzy matching algorithms)
├── Repositories/Jpa/
│   ├── Filter/
│   │   ├── *FilterRepository.java     (Interface)
│   │   └── *FilterRepositoryImpl.java (Implementation)
│   └── Search/
│       ├── *SearchRepository.java     (Interface)
│       └── *SearchRepositoryImpl.java (Implementation)
├── Mappers/
│   ├── AccommodationFilterMapper.java        (Interface)
│   ├── AccommodationFilterMapperImpl.java   (@Component)
│   ├── TourFilterMapper.java                (Interface)
│   ├── TourFilterMapperImpl.java           (@Component)
│   ├── TransportTicketFilterMapper.java     (Interface)
│   ├── TransportTicketFilterMapperImpl.java (@Component)
│   ├── VoucherFilterMapper.java             (Interface)
│   └── VoucherFilterMapperImpl.java         (@Component)
├── Utils/
│   └── VietnameseSearchUtils.java      (Vietnamese accent removal utility)
├── Dto/Request/
│   ├── Filter/
│   │   ├── AccommodationFilterRequest.java
│   │   ├── TourFilterRequest.java
│   │   ├── TransportTicketFilterRequest.java
│   │   └── VoucherFilterRequest.java
│   └── Search/
│       ├── AccommodationSearchRequest.java
│       ├── TourSearchRequest.java
│       ├── TransportTicketSearchRequest.java
│       └── VoucherSearchRequest.java
├── Dto/Response/
│   ├── AccommodationFilterResponse.java
│   ├── TourFilterResponse.java
│   ├── TourScheduleResponse.java
│   ├── TransportTicketFilterResponse.java
│   ├── VoucherFilterResponse.java
│   └── FilterPageResponse.java
├── Constant/
│   └── FilterConstant.java           (All constants, enums, allowed values)
└── Docs/
    └── ITEM_FILTER_SYSTEM.md
```

---

## FilterConstant Enums

```java
// Accommodation Types
HOTEL, HOSTEL, RESORT, VILLA, APARTMENT, HOMESTAY, MOTEL

// Ticket Types
PLANE, BUS, TRAIN, FERRY, CAR_RENTAL

// Ticket Classes
ECONOMY, BUSINESS, FIRST, PREMIUM_ECONOMY

// Sponsor Types
SYSTEM, BUSINESS

// Apply Types
ALL, ROOM, SERVICE, TOUR, TICKET

// Duration Types
HALF_DAY, FULL_DAY, MULTIPLE_DAYS

// Tour Types
ADVENTURE, CULTURAL, BEACH, MOUNTAIN, CITY_TOUR, ECOTOURISM, FOOD_TOUR

// Difficulty
EASY, MODERATE, CHALLENGING

// Sort Direction
ASC, DESC

// Sort Relevance
"relevance" (default for search)
```
