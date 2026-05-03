# Hệ thống phân khúc khách hàng (Customer Segmentation)

## Tổng quan

Hệ thống phân khúc khách hàng sử dụng AI để tự động phân loại người dùng vào 5 nhóm dựa trên hành vi, sở thích và lịch sử đặt phòng. Điều này giúp các đối tác kinh doanh hiểu rõ cơ sở khách hàng của mình và điều chỉnh chiến lược phù hợp.

## Các nhóm khách hàng

| Nhóm | Mô tả | Đặc điểm chính |
|------|-------|----------------|
| **FAMILY** | Du lịch gia đình | Có con nhỏ, đặt chỗ ở thân thiện với gia đình, ưu tiên an toàn, đặt trước 1-2 tuần, thường đặt nhiều phòng |
| **COUPLES** | Cặp đôi/honeymoon | 2 người đi, điểm đến lãng mạn, quan tâm riêng tư, booking value cao |
| **GROUP** | Du lịch nhóm | >3 người, hoạt động mạo hiểm, nhạy cảm về giá, lịch trình linh hoạt, hay đặt nhiều tour |
| **TOURIST** | Khách du lịch thông thường | Một mình hoặc hoạt động hỗn hợp, sightseeing, đa dạng loại hình, không cố định |
| **BUSINESS** | Khách công tác | Short stay, work-focused amenities, booking gấp, highest avg booking value |

## Cấu trúc Database

### Bảng users - Các cột mới

```sql
ALTER TABLE users ADD COLUMN customer_segment VARCHAR(50);
ALTER TABLE users ADD COLUMN segment_confidence DOUBLE PRECISION;
ALTER TABLE users ADD COLUMN segment_analyzed_at TIMESTAMP;
```

### Entity Fields

```java
@Column(name = "customer_segment", length = 50)
private String customerSegment;
// Giá trị: FAMILY | COUPLES | GROUP | TOURIST | BUSINESS

@Column(name = "segment_confidence")
private Double segmentConfidence;
// Độ tin confidence: 0.0 - 1.0 (ví dụ: 0.85 = 85%)

@Column(name = "segment_analyzed_at")
private LocalDateTime segmentAnalyzedAt;
// Thời điểm phân tích gần nhất
```

## Kiến trúc sự kiện (Event-Driven Architecture)

### Vấn đề ban đầu: Circular Dependency

```
CustomerSegmentationService → ICustomerSegmentationAgent → BookingChatTools → BookingTourService → CustomerSegmentationService
                    ↑                                                                                              ↓
                    └──────────────────────────────────────────────────────────────────────────────┘
```

### Giải pháp: Spring ApplicationEventPublisher

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                    SERVICE GỐC (TRIGGER)                                │
│                                                                                         │
│   UserService                    → Phát sinh khi đăng ký user mới                      │
│   UserPreferencesService         → Phát sinh khi cập nhật sở thích                      │
│   BookingHandleService           → Phát sinh khi checkout accommodation                 │
│   BookingTourService             → Phát sinh khi booking tour thành công                │
│   BookingTransportTicketService   → Phát sinh khi booking transport thành công           │
└─────────────────────────────────────────────────────────────────────────────────────────┘
                                          │
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              BookingCompletedEvent                                      │
│                                                                                         │
│   eventPublisher.publishEvent(                                                          │
│       BookingCompletedEvent.builder()                                                   │
│           .userId(userId)                                                               │
│           .bookingType("ACCOMMODATION" | "TOUR" | "TRANSPORT")                          │
│           .bookingId(bookingId)                                                         │
│           .build()                                                                      │
│   );                                                                                    │
└─────────────────────────────────────────────────────────────────────────────────────────┘
                                          │
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                    CustomerSegmentationService (Event Listener)                          │
│                              @Async + @EventListener                                    │
│                                                                                         │
│   @Async("segmentationExecutor")                                                        │
│   @EventListener                                                                         │
│   public void handleBookingCompletedEvent(BookingCompletedEvent event) {                │
│       analyzeAndUpdateSegmentAsync(event.getUserId());                                  │
│   }                                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

## Luồng cập nhật 3 trường phân khúc

### Tổng quan xử lý

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Bước 1: Tiếp nhận sự kiện                                                      │
│          CustomerSegmentationService nhận BookingCompletedEvent qua @EventListener │
│                                                                                 │
│  Bước 2: Kiểm tra skip (caching strategy)                                       │
│          shouldSkipAnalysis(user)                                                │
│          - Confidence > 0.8 và chưa quá 6 tháng → BỎ QUA                         │
│          - Confidence > 0.5 và chưa quá 3 tháng → BỎ QUA                         │
│          - Segment cũ > 6 tháng → PHÂN TÍCH                                     │
│          - Chưa có segment → PHÂN TÍCH                                          │
│                                                                                 │
│  Bước 3: Lấy dữ liệu phân tích (TỪ 3 NGUỒN)                                     │
│          UserAnalyticsService.buildUserAnalyticsData(userId)                     │
│          - Booking (Accommodation): totalPrice, checkinDate, checkoutDate       │
│          - BookingTour: priceBookingTour, createdAt                             │
│          - BookingTransport: priceBookingTransport, createdAt                  │
│          - Tổng hợp: totalSpent, bookingCount, totalTrips                       │
│          - Sở thích: travelStyle, budgetLevel, travelingWith, hasChildren       │
│                                                                                 │
│  Bước 4: Gọi AI phân tích (Groq Llama)                                          │
│          ICustomerSegmentationAgent.analyzeCustomer(userDataJson)                │
│          AI trả về JSON: {segment, confidence, reasoning}                        │
│                                                                                 │
│  Bước 5: Parse AI response                                                       │
│          parseSegmentationResponse(aiResponse)                                  │
│                                                                                 │
│  Bước 6: Kiểm tra có nên update không                                            │
│          shouldUpdateSegment(user, newSegment, newConfidence)                   │
│          - Segment khác nhau: newConfidence > currentConfidence + 0.15           │
│          - Segment giống nhau: newConfidence > currentConfidence + 0.15         │
│                                                                                 │
│  Bước 7: Cập nhật database                                                       │
│          UserRepository.updateCustomerSegment(userId, segment,                  │
│                                              confidence, LocalDateTime.now())    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Chi tiết các điểm trigger

#### 1. UserService.saveUser() - Dòng 184-186
```java
// Sau khi lưu user mới thành công
eventPublisher.publishEvent(BookingCompletedEvent.builder()
        .userId(savedUser.getIdUser())
        .build());
```

#### 2. UserPreferencesService.createOrUpdate() - Dòng 105-107
```java
// Sau khi lưu preferences thành công
eventPublisher.publishEvent(BookingCompletedEvent.builder()
        .userId(userUuid)
        .build());
```

#### 3. BookingHandleService - Dòng 859 (checkout booking)
```java
eventPublisher.publishEvent(BookingCompletedEvent.builder()
        .userId(booking.getUsers().getIdUser())
        .bookingType("ACCOMMODATION")
        .bookingId(booking.getIdBooking())
        .build());
```

#### 4. BookingTourService - Dòng 388 (booking tour thành công)
```java
eventPublisher.publishEvent(BookingCompletedEvent.builder()
        .userId(bookingTour.getUsers().getIdUser())
        .bookingType("TOUR")
        .bookingId(bookingTour.getIdBookingTour())
        .build());
```

#### 5. BookingTransportTicketService - Dòng 371 (booking transport thành công)
```java
eventPublisher.publishEvent(BookingCompletedEvent.builder()
        .userId(booking.getUsers().getIdUser())
        .bookingType("TRANSPORT")
        .bookingId(booking.getIdBookingTransport())
        .build());
```

### Dữ liệu gửi cho AI (UserAnalyticsDataResponse)

```java
public class UserAnalyticsDataResponse {
    private String userId;
    private String firstName;
    private String lastName;
    private Integer loyaltyPoints;
    private String currentSegment;
    private PreferencesDTO preferences;
    
    // Metrics tổng hợp
    private Integer totalBookings;      // Tổng số booking đã hoàn thành (cả 3 loại)
    private Long totalSpent;            // Tổng chi tiêu (cả 3 loại)
    private Integer avgBookingValue;     // Giá trị trung bình mỗi booking
    private Integer totalNights;        // Tổng số đêm lưu trú (chỉ từ accommodation)
    private Integer totalTrips;         // Tổng số chuyến đi đã hoàn thành
    
    // Chi tiết từng loại booking
    private Integer accommodationCount;
    private Long accommodationTotalSpent;
    private Integer tourCount;
    private Long tourTotalSpent;
    private Integer transportCount;
    private Long transportTotalSpent;
    
    private Map<String, Integer> recentBookingMonths;
    private List<BookingSummaryDTO> recentBookings;  // Giới hạn 15 booking gần nhất
}

public class BookingSummaryDTO {
    private String bookingType;   // "ACCOMMODATION" | "TOUR" | "TRANSPORT"
    private String status;
    private Double totalPrice;
    private String checkinDate;
    private String checkoutDate;
    private String accommodationType;
}
```

### Logic kiểm tra Skip

#### shouldSkipAnalysis() - Có nên bỏ qua phân tích không?

```java
private boolean shouldSkipAnalysis(Users user) {
    // Chưa có segment → cần phân tích ngay
    if (user.getCustomerSegment() == null || user.getSegmentConfidence() == null) {
        return false;
    }

    // Confidence cao (>0.8) và chưa cũ → bỏ qua
    if (user.getSegmentConfidence() > 0.8 && !isSegmentStale(user)) {
        return true;
    }

    // Confidence trung bình (>0.5) và gần đây (<3 tháng) → bỏ qua
    if (user.getSegmentConfidence() > 0.5 && user.getSegmentAnalyzedAt() != null) {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        if (user.getSegmentAnalyzedAt().isAfter(threeMonthsAgo)) {
            return true;
        }
    }

    return false; // Cần phân tích lại
}

private boolean isSegmentStale(Users user) {
    if (user.getSegmentAnalyzedAt() == null) {
        return true;
    }
    LocalDateTime staleDate = LocalDateTime.now().minusMonths(6);
    return user.getSegmentAnalyzedAt().isBefore(staleDate);
}
```

### Logic kiểm tra Update

#### shouldUpdateSegment() - Có nên cập nhật segment không?

```java
private boolean shouldUpdateSegment(Users user, String newSegment, double newConfidence) {
    // Chưa có segment → cập nhật ngay
    if (user.getCustomerSegment() == null) {
        return true;
    }

    // Segment khác nhau → chỉ update nếu confidence cải thiện đáng kể
    if (!user.getCustomerSegment().equals(newSegment)) {
        return newConfidence > user.getSegmentConfidence() + 0.15;
    }

    // Segment giống nhau → chỉ update nếu confidence tăng thêm 0.15
    return newConfidence > user.getSegmentConfidence() + 0.15;
}
```

### Luồng AI phân tích và cập nhật 3 trường

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         AI PHÂN TÍCH XONG                                    │
│                                                                              │
│   AI trả về JSON:                                                            │
│   {                                                                          │
│     "segment": "FAMILY",      ← customerSegment (String)                    │
│     "confidence": 0.85,       ← segmentConfidence (Double)                  │
│     "reasoning": "..."        ← (không lưu vào database)                    │
│   }                                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│              CustomerSegmentationService.parseSegmentationResponse()        │
│                         Parse JSON thành object                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                  shouldUpdateSegment() - Kiểm tra có nên update không       │
│                                                                              │
│   Ví dụ:                                                                      │
│   - User hiện tại: segment="TOURIST", confidence=0.7                        │
│   - AI mới: segment="FAMILY", confidence=0.85                               │
│   - Kiểm tra: 0.85 > 0.7 + 0.15 → true → UPDATE                             │
│                                                                              │
│   - User hiện tại: segment="FAMILY", confidence=0.85                        │
│   - AI mới: segment="GROUP", confidence=0.80                                 │
│   - Kiểm tra: 0.80 > 0.85 + 0.15 → false → SKIP (giữ nguyên)                │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Database UPDATE                                        │
│                                                                              │
│   UserRepository.updateCustomerSegment(                                      │
│       userId,                                                                 │
│       result.getSegment(),      // "FAMILY"                                  │
│       result.getConfidence(),   // 0.85                                       │
│       LocalDateTime.now()       // segmentAnalyzedAt                         │
│   );                                                                          │
│                                                                              │
│   SQL:                                                                        │
│   UPDATE users SET                                                            │
│     customer_segment = 'FAMILY',                                             │
│     segment_confidence = 0.85,                                               │
│     segment_analyzed_at = '2026-05-03 16:40:00'                              │
│   WHERE id_user = 'xxx';                                                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Database Update Query

```java
@Modifying
@Query("UPDATE Users u SET u.customerSegment = :segment, " +
       "u.segmentConfidence = :confidence, u.segmentAnalyzedAt = :analyzedAt " +
       "WHERE u.idUser = :userId")
void updateCustomerSegment(UUID userId, String segment, Double confidence, LocalDateTime analyzedAt);
```

### Ví dụ cụ thể các tình huống

| Tình huống | Segment hiện tại | Confidence hiện tại | AI Response | Kết quả |
|------------|------------------|---------------------|-------------|---------|
| User mới | null | null | {segment: "TOURIST", confidence: 0.75} | **UPDATE** |
| Cải thiện đáng kể | "TOURIST" | 0.7 | {segment: "FAMILY", confidence: 0.88} | **UPDATE** (0.88 > 0.85) |
| Cùng segment, cải thiện | "FAMILY" | 0.8 | {segment: "FAMILY", confidence: 0.92} | **UPDATE** (0.92 > 0.95) |
| Không đủ cải thiện | "FAMILY" | 0.85 | {segment: "GROUP", confidence: 0.80} | **SKIP** (0.80 < 1.00) |
| Confidence cao, gần đây | "COUPLES" | 0.9 | - | **SKIP** (đủ tự tin) |

## Chiến lược tối ưu Token (Caching + Threshold)

### Điều kiện Skip

| Điều kiện | Hành động |
|-----------|-----------|
| `confidence > 0.8` VÀ analyzed < 6 tháng | **BỎ QUA** - Đủ tự tin |
| `0.5 < confidence <= 0.8` VÀ analyzed < 3 tháng | **BỎ QUA** - Gần đây |
| Segment data cũ (> 6 tháng) | **PHÂN TÍCH** - Cần làm mới |
| Chưa có segment | **PHÂN TÍCH** - Lần đầu |

### Quy tắc Recalculation

- Nếu segment mới khác segment hiện tại VÀ `newConfidence > currentConfidence + 0.15`: **CẬP NHẬT**
- Nếu cùng segment VÀ `newConfidence > currentConfidence + 0.15`: **CẬP NHẬT**
- Ngược lại: **BỎ QUA**

## Tích hợp AI

### Cấu hình Model

- **Nhà cung cấp**: Groq
- **Model**: `meta-llama/llama-4-scout-17b-16e-instruct`
- **Temperature**: 0.3 (thấp để phân loại nhất quán)
- **Max Tokens**: 500

### Prompt Template (ICustomerSegmentationAgent)

```
Bạn là chuyên gia phân tích phân khúc khách hàng du lịch.

IMPORTANT: KHÔNG ĐƯỢC GỌI BẤT KỲ TOOL NÀO. CHỈ PHÂN TÍCH DỮ LIỆU VÀ TRẢ JSON.
IMPORTANT: Chỉ trả về duy nhất JSON, KHÔNG có text giải thích trước hoặc sau JSON.

Phân loại khách hàng vào 1 trong 5 nhóm:
- FAMILY: Đi du lịch gia đình, có con nhỏ, ưu tiên an toàn, tiện nghi, booking trước 1-2 tuần, thường đặt nhiều phòng
- COUPLES: Cặp đôi, honeymoon, ưu tiên romantic, riêng tư, đi 2 người, booking value cao
- GROUP: Nhóm bạn >3 người, ưu tiên adventure, giá rẻ, flexible, hay đặt nhiều tour
- TOURIST: Du lịch đơn lẻ hoặc hỗn hợp, sightseeing, đa dạng loại hình, không cố định
- BUSINESS: Công tác, short stay, work-focused amenities, booking gấp, highest avg booking value

Dữ liệu khách hàng bao gồm:
- Thông tin cá nhân: tên, loyalty points, segment hiện tại
- Sở thích: travelStyle, budgetLevel, travelingWith, hasChildren, interests, preferredActivities
- Lịch sử booking:
  * accommodationCount/tourCount/transportCount - số lần đặt từng loại
  * accommodationTotalSpent/tourTotalSpent/transportTotalSpent - tổng chi tiêu từng loại
  * totalTrips - tổng số chuyến đi đã hoàn thành
  * totalNights - tổng số đêm lưu trú (chỉ từ accommodation)
  * avgBookingValue - giá trị trung bình mỗi booking
- Recent bookings: danh sách 15 booking gần nhất với bookingType (ACCOMMODATION/TOUR/TRANSPORT)

Phân tích dựa trên:
1. Tỷ lệ chi tiêu giữa accommodation/tour/transport
2. Số lượng người đi (travelingWith, hasChildren)
3. Pattern booking (advance booking vs last minute)
4. Giá trị trung bình booking
5. Thời gian lưu trú/trip duration

Trả lời JSON format:
{
  "segment": "FAMILY|COUPLES|GROUP|TOURIST|BUSINESS",
  "confidence": 0.0-1.0,
  "reasoning": "Mô tả ngắn tại sao phân loại như vậy (tối đa 100 ký tự)"
}
```

### AI Response Format mong đợi

```json
{
  "segment": "FAMILY",
  "confidence": 0.85,
  "reasoning": "User book gia đình 4 người, có children, ưu tiên resort an toàn"
}
```

### Ví dụ dữ liệu đầy đủ gửi cho AI

```json
{
  "userId": "xxx-xxx-xxx",
  "firstName": "Nguyễn",
  "lastName": "Văn A",
  "loyaltyPoints": 1500,
  "currentSegment": "TOURIST",
  "preferences": {
    "travelStyle": "adventure",
    "budgetLevel": "moderate",
    "travelingWith": "friends",
    "hasChildren": false,
    "interests": ["beach", "museum"],
    "preferredActivities": ["swimming", "hiking"],
    "preferredAccommodation": "hotel",
    "isCompleted": true
  },
  "totalBookings": 12,
  "totalSpent": 25000000,
  "avgBookingValue": 2083333,
  "totalNights": 15,
  "totalTrips": 12,
  "accommodationCount": 5,
  "accommodationTotalSpent": 15000000,
  "tourCount": 4,
  "tourTotalSpent": 6000000,
  "transportCount": 3,
  "transportTotalSpent": 4000000,
  "recentBookingMonths": {
    "2026-03": 2,
    "2026-02": 3,
    "2026-01": 2
  },
  "recentBookings": [
    {"bookingType": "ACCOMMODATION", "status": "Success", "totalPrice": 3000000, ...},
    {"bookingType": "TOUR", "status": "Success", "totalPrice": 2000000, ...},
    {"bookingType": "TRANSPORT", "status": "Success", "totalPrice": 1500000, ...}
  ]
}
```

## API Analytics cho Business

Base path: `/api/business/analytics`

### Các endpoint

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/segments?businessId={uuid}&months=6` | Phân bổ nhóm khách hàng |
| GET | `/segments/chart?businessId={uuid}&months=6` | Dữ liệu biểu đồ (cho pie/bar charts) |
| GET | `/segments/{segment}?businessId={uuid}&months=6` | Chi tiết 1 nhóm khách hàng |
| GET | `/insights?businessId={uuid}&months=3` | Insights tự động (rule-based) |
| GET | `/insights/ai?businessId={uuid}&months=3` | Khuyến nghị từ AI (Groq Llama) |

**Lưu ý:** Các API `/overview` và `/trends` đã được loại bỏ vì trùng lặp với báo cáo bên Golang service.

### Ví dụ Response

#### GET /api/business/analytics/segments

```json
{
  "periodMonths": 6,
  "segments": {
    "FAMILY": {
      "count": 45,
      "totalRevenue": 121500000,
      "avgBookingValue": 2700000,
      "percentage": 28.8
    },
    "COUPLES": {
      "count": 32,
      "totalRevenue": 96000000,
      "avgBookingValue": 3000000,
      "percentage": 20.5
    },
    "GROUP": {
      "count": 18,
      "totalRevenue": 72000000,
      "avgBookingValue": 4000000,
      "percentage": 11.5
    },
    "TOURIST": {
      "count": 56,
      "totalRevenue": 140000000,
      "avgBookingValue": 2500000,
      "percentage": 35.9
    },
    "BUSINESS": {
      "count": 5,
      "totalRevenue": 20000000,
      "avgBookingValue": 4000000,
      "percentage": 3.2
    }
  },
  "totalCustomers": 156,
  "totalRevenue": 449500000
}
```

#### GET /api/business/analytics/insights

```json
{
  "summary": "Doanh thu: 449500000 VND, 156 bookings, Top segment: TOURIST",
  "insights": [
    "Nhóm TOURIST chiếm tỷ lệ cao nhất (35.9%) trong khách hàng của bạn",
    "Nhóm BUSINESS có avg booking value cao nhất: 4000000 VND",
    "Nhóm FAMILY ổn định - nên giữ chương trình hiện tại"
  ],
  "recommendations": [
    {
      "segment": "BUSINESS",
      "action": "Thêm business center, wifi tốc độ cao, meeting rooms để thu hút khách công tác",
      "priority": "HIGH"
    },
    {
      "segment": "FAMILY",
      "action": "Tạo family packages với kids activities, giảm giá cho trẻ em",
      "priority": "MEDIUM"
    }
  ]
}
```

## Cấu trúc file

```
Services/
├── CustomerSegmentationService.java      # Logic phân khúc chính (event listener)
├── UserAnalyticsService.java             # Tổng hợp dữ liệu từ 3 loại booking
├── BusinessAnalyticsService.java         # Query analytics cho business
└── BusinessInsightsService.java          # Khuyến nghị từ AI

Services/Interfaces/
└── ICustomerSegmentationAgent.java       # Interface @AiService với prompt chi tiết

Helpers/
└── SegmentationTools.java                # @Tool cho AI queries

Controllers/
└── BusinessAnalyticsController.java       # Các endpoint API analytics

Dto/
├── Request/
│   └── CustomerSegmentationRequest.java
└── Response/
    ├── CustomerSegmentationResponse.java
    ├── SegmentDistributionResponse.java
    ├── SegmentChartResponse.java
    ├── BusinessInsightsResponse.java
    ├── SegmentDetailResponse.java
    └── UserAnalyticsDataResponse.java     # Đã cập nhật với tour/transport stats

Configs/
└── AsyncConfig.java                      # Thread pool cho xử lý async

Events/
└── BookingCompletedEvent.java            # Event để break circular dependency

Repositories/Jpa/
└── UserRepository.java                   # Method updateCustomerSegment()
```

**Các file đã xóa:** BusinessOverviewResponse.java, MonthlyTrendResponse.java

## Phòng tránh N+1 Query

Hệ thống sử dụng các chiến lược sau để tránh N+1 queries:

1. **BatchSize**: `@BatchSize(size = 100)` trên entity Users
2. **Query Optimization**: Sử dụng `JOIN FETCH` trong repository methods
3. **Pagination**: Sử dụng `Pageable` cho batch operations
4. **Async Processing**: Thread pool riêng cho các tác vụ phân khúc

---

## Lịch sử cập nhật

| Ngày | Mô tả | File thay đổi |
|------|-------|---------------|
| 2026-05-03 | Fix Transaction Boundary Issue - di chuyển @Transactional từ service sang repository | UserRepository.java, CustomerSegmentationService.java |
| 2026-05-03 | Fix Tool Leakage Issue - thêm instruction cấm gọi tool trong SystemMessage | ICustomerSegmentationAgent.java |

**Last Updated**: 2026-05-03

## Xử lý lỗi

- Nếu AI call thất bại: Ghi log error, bỏ qua phân tích (không retry)
- Nếu user không tìm thấy: Ghi log warning, bỏ qua
- Nếu AI response không hợp lệ: Ghi log warning, bỏ qua

## Các lỗi đã xử lý (Bugs Fixed)

### 1. Transaction Boundary Issue

**Mô tả lỗi**: Khi `@Async` method gọi `@Transactional` method trong thread khác, transaction proxy không hoạt động → lỗi "Executing an update/delete query".

**Stack trace lỗi**:
```
@Async analyzeAndUpdateSegmentAsync() [segment-1 thread]
  └─→ analyzeAndUpdateSegment()
        └─→ performSegmentUpdate() [@Transactional] ← Proxy fails!
              └─→ userRepository.updateCustomerSegment()
                    ❌ No transaction context = ERROR
```

**Giải pháp**: Di chuyển `@Transactional` từ service method sang repository method.

**File thay đổi**:
- `UserRepository.java` (line 34-36): Thêm `@Transactional` vào method `updateCustomerSegment()`
- `CustomerSegmentationService.java` (line 93): Loại bỏ `@Transactional` khỏi method `performSegmentUpdate()`

**Trạng thái**: ✅ Đã fix (2026-05-03)

---

### 2. Tool Leakage Issue

**Mô tả lỗi**: AI model (Llama) gọi nhầm tool `getVouchersByAccommodation` từ `BookingChatTools` trong khi `ICustomerSegmentationAgent` không có tools nào được chỉ định.

**Log lỗi**:
```
ERROR 9980 --- [java-service] [      segment-1] c.e.j.Helpers.BookingChatTools : Tool Error - getVouchersByAccommodation: Không tìm thấy người dùng!
```

**Root cause**: LangChain4j `@AiService` auto-discovery expose TẤT CẢ `@Tool` methods trong Spring context cho MỌI `@AiService`. `BookingChatTools` là `@Component` với 40+ `@Tool` methods → bị expose cho `ICustomerSegmentationAgent`.

**Giải pháp**: Thêm instruction trong `@SystemMessage` của `ICustomerSegmentationAgent` để cấm gọi tool.

**File thay đổi**:
- `ICustomerSegmentationAgent.java` (line 11-13): Thêm `IMPORTANT: KHÔNG ĐƯỢC GỌI BẤT KỲ TOOL NÀO. CHỈ PHÂN TÍCH DỮ LIỆU VÀ TRẢ JSON.`

**Trạng thái**: ✅ Đã fix (2026-05-03)

---

## Các cải tiến trong tương lai

- [ ] Retry mechanism cho các AI call thất bại
- [ ] Scheduled batch recalculation cho các segment cũ
- [ ] Thông báo segment change cho các businesses
- [ ] A/B testing cho các segmentation thresholds