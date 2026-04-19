# Itinerary & ItineraryItem Module Specification

## 1. Overview

Module Itinerary quản lý lịch trình du lịch của người dùng. Mỗi itinerary chứa các items đại diện cho các hoạt động trong chuyến đi (住宿, 交通, 餐饮, 活动, 游览).

## 2. Entities

### Itinerary.java

```java
@Entity
@Table(name = "itinerary")
public class Itinerary {
    UUID idItinerary;                    // Primary Key
    Users users;                         // Người tạo (FK -> Users)
    String destination;                  // Điểm đến
    LocalDate startDate;                // Ngày bắt đầu
    LocalDate endDate;                  // Ngày kết thúc
    BigDecimal budget;                  // Ngân sách dự kiến
    String status;                      // Trạng thái itinerary
    BigDecimal totalEstimatedCost;       // Tổng chi phí ước tính
    String currency;                    // Tiền tệ (default: VND)
    String notes;                       // Ghi chú
    LocalDateTime createdAt;            // Ngày tạo
}
```

### ItineraryItem.java

```java
@Entity
@Table(name = "itinerary_item")
public class ItineraryItem {
    UUID idItem;                        // Primary Key
    Itinerary itinerary;                // Itinerary cha (FK -> Itinerary)
    String itemType;                   // Loại item (xem PredefinedItemType)
    LocalDate scheduledDate;            // Ngày thực hiện
    LocalTime startTime;                // Giờ bắt đầu
    LocalTime endTime;                 // Giờ kết thúc
    UUID refId;                         // Reference ID từ DB (nullable)
    String suggestedName;               // Tên đề xuất
    String suggestedLocation;           // Địa điểm đề xuất
    BigDecimal suggestedPrice;          // Giá ước tính
    Integer estimatedDurationMinutes;   // Thời lượng ước tính (phút)
    Integer displayOrder;               // Thứ tự hiển thị
    LocalDateTime createdAt;            // Ngày tạo
}
```

## 3. Constants

### PredefinedItemType.java
```java
FLIGHT          // Máy bay
ACCOMMODATION   // Nơi cư trú
TOUR            // Tour du lịch
TRANSPORT       // Phương tiện di chuyển
MEAL            // Bữa ăn
ACTIVITY        // Hoạt động
```

### PredefinedStatusItinerary.java
```java
DRAFT           // Bản nháp
AI_GENERATED    // Được tạo bởi AI
CONFIRMED       // Đã xác nhận
COMPLETED       // Hoàn thành
CANCELLED       // Đã hủy
```

### PredefinedStatusItineraryItem.java
```java
PENDING         // Chờ xử lý
BOOKED          // Đã đặt
CANCELLED       // Đã hủy
```

## 4. API Endpoints

Base Path: `/itinerary`

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `` | Tạo itinerary mới |
| GET | `` | Lấy thông tin itinerary |
| GET | `/user?idUser={uuid}` | Lấy danh sách itinerary của user |
| PUT | `?idItinerary={uuid}` | Cập nhật itinerary |
| DELETE | `?idItinerary={uuid}` | Xóa itinerary |
| PATCH | `/confirm?idItinerary={uuid}` | Xác nhận itinerary |

Base Path: `/itinerary/items`

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/list?idItinerary={uuid}` | Lấy danh sách items |
| GET | `/inf?idItem={uuid}` | Lấy thông tin item |
| POST | `` | Tạo item mới |
| PUT | `?idItem={uuid}` | Cập nhật item |
| PATCH | `/status?idItem={uuid}` | Cập nhật trạng thái item |
| PATCH | `/reorder` | Sắp xếp lại thứ tự items |
| DELETE | `?idItem={uuid}` | Xóa item |

## 5. Service Layer

### ItineraryService.java

| Method | Mô tả |
|--------|--------|
| `createItinerary(CreateItineraryRequest request)` | Tạo itinerary mới |
| `getItinerary(String idItinerary)` | Lấy thông tin itinerary |
| `getUserItineraries(String idUser)` | Lấy danh sách itinerary của user |
| `updateItinerary(String idItinerary, UpdateItineraryRequest request)` | Cập nhật itinerary |
| `deleteItinerary(String idItinerary)` | Xóa itinerary |
| `confirmItinerary(String idItinerary)` | Xác nhận itinerary |

### ItineraryItemService (via ItineraryService)

| Method | Mô tả |
|--------|--------|
| `getItineraryItems(String idItinerary)` | Lấy danh sách items |
| `createItineraryItem(CreateItineraryItemRequest request)` | Tạo item mới |
| `updateItineraryItem(String idItem, UpdateItineraryItemRequest request)` | Cập nhật item |
| `deleteItineraryItem(String idItem)` | Xóa item |
| `reorderItems(ReorderItemsRequest request)` | Sắp xếp lại thứ tự |
| `updateItemStatus(String idItem, String status)` | Cập nhật trạng thái item |

## 6. Repository

### ItineraryRepository.java
```java
findByUsers_IdUserOrderByCreatedAtDesc(UUID idUser);
```

### ItineraryItemRepository.java
```java
findByItinerary_IdItineraryOrderByDisplayOrderAsc(UUID idItinerary);
findByItinerary_IdItineraryAndRefId(UUID idItinerary, UUID refId);
deleteByItinerary_IdItinerary(UUID idItinerary);
findMaxDisplayOrderByItineraryId(UUID idItinerary);
decrementDisplayOrderForItemsAfter(UUID idItinerary, int order);
```

## 7. DTOs

### Itinerary DTOs
- `CreateItineraryRequest` - Tạo itinerary
- `UpdateItineraryRequest` - Cập nhật itinerary
- `ItineraryResponse` - Response itinerary

### ItineraryItem DTOs
- `CreateItineraryItemRequest` - Tạo item
- `UpdateItineraryItemRequest` - Cập nhật item
- `ReorderItemsRequest` - Sắp xếp lại
- `ItineraryItemResponse` - Response item

## 8. AI Schedule Integration

Itinerary được tích hợp với AI Scheduler (xem SPEC_AI_SCHEDULE.md):

```
1. User tạo itinerary với destination, dates, budget, preferences
         ↓
2. AI Scheduler generate schedule (LLM Call #1)
   - Tạo items với suggestedName, suggestedPrice, duration
   - refId = null (chưa match với DB)
         ↓
3. AI Scheduler fetch DB data theo "needs"
         ↓
4. AI Scheduler enrich items (LLM Call #2)
   - Match accommodation/tour/transport với DB
   - Update refId nếu tìm thấy
   - Update suggestedPrice từ DB nếu có refId
         ↓
5. Items saved với status = AI_GENERATED
```

## 9. Item Duration Guidelines (AI Scheduler)

| Type | Duration |
|------|----------|
| ACCOMMODATION | 1440 phút (24 tiếng = 1 đêm) |
| TRANSPORT | Theo DB hoặc 45-60 phút/50km |
| MEAL | 45-90 phút |
| ACTIVITY | 60-180 phút |
| TOUR | 240-480 phút (4-8 tiếng) |
| FLIGHT | 60-180 phút |

## 10. Display Order Logic

```java
// Khi tạo item mới
maxOrder = findMaxDisplayOrderByItineraryId(itineraryId);
newItem.displayOrder = maxOrder + 1;

// Khi xóa item
// 1. Xóa item
// 2. Decrement displayOrder của các item phía sau

// Khi reorder
// 1. Update displayOrder của các items affected
// 2. Đảm bảo không có gap trong sequence
```

## 11. File Locations

| Type | Path |
|------|------|
| Entities | `Models/Itinerary.java` |
| | `Models/ItineraryItem.java` |
| Repositories | `Repositories/Jpa/ItineraryRepository.java` |
| | `Repositories/Jpa/ItineraryItemRepository.java` |
| Services | `Services/ItineraryService.java` |
| Controllers | `Controllers/ItineraryController.java` |
| | `Controllers/ItineraryItemController.java` |
| Mappers | `Mappers/ItineraryMapper.java` |
| | `Mappers/ItineraryItemMapper.java` |
| Constants | `Constant/PredefinedItemType.java` |
| | `Constant/PredefinedStatusItinerary.java` |
| | `Constant/PredefinedStatusItineraryItem.java` |

## 12. Status Flow

```
Itinerary Status:
DRAFT -> AI_GENERATED -> CONFIRMED -> COMPLETED
                     -> CANCELLED

Item Status:
PENDING -> BOOKED
        -> CANCELLED
```
