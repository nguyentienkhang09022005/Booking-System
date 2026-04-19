# Tour Module Specification

## 1. Overview

Module Tour quản lý các tour du lịch trong hệ thống BookingNext. Tour là gói dịch vụ du lịch trọn gói bao gồm lịch trình, điểm đón/trả, và các dịch vụ đi kèm.

## 2. Entity

### Tour.java

```java
@Entity
@Table(name = "tour")
public class Tour {
    UUID idTour;              // Primary Key
    Users users;              // Công ty cung cấp tour (FK -> Users)
    String tourName;          // Tên tour
    String description;       // Mô tả chi tiết
    Integer durationMinutes;  // Thời lượng tour (phút)
    String meetingPoint;      // Điểm đón
    String dropOffPoint;      // Điểm trả
    String cancellationPolicy; // Chính sách hủy
    String languages;         // Ngôn ngữ hỗ trợ
    List<String> inclusions;  // Bao gồm (jsonb)
    List<String> exclusions;  // Không bao gồm (jsonb)
    List<String> additionalInfo; // Thông tin bổ sung (jsonb)
    BigDecimal adultPrice;   // Giá người lớn
    BigDecimal childPrice;   // Giá trẻ em
    String status;            // Trạng thái tour
    Boolean approved;         // Đã phê duyệt
    LocalDateTime createdAt; // Ngày tạo
    
    @OneToMany(mappedBy = "tour")
    List<TourSchedule> tourSchedules; // Lịch trình tour
}
```

## 3. Constants

### PredefinedStatusTour.java
```java
STATUS_TOUR_AVAILABLE   // Tour có sẵn
STATUS_TOUR_SOLD_OUT    // Tour hết chỗ
```

### PredefinedStatusBookingTour.java
```java
STATUS_BOOKING_PENDING  // Chờ thanh toán
STATUS_BOOKING_SUCCESS  // Thanh toán thành công
STATUS_BOOKING_FAILED   // Thanh toán thất bại
```

## 4. API Endpoints

Base Path: `/tour`

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/detail?idTour={uuid}` | Lấy chi tiết tour |
| GET | `/get-all` | Lấy tất cả tour |
| GET | `/get-list-approved` | Lấy tour đã phê duyệt |
| GET | `/get-list-not-approved` | Lấy tour chưa phê duyệt |
| GET | `/list-of-user?idUser={uuid}` | Lấy tour của người dùng |
| POST | `/creation` | Tạo tour mới |
| PATCH | `/update?idTour={uuid}` | Cập nhật tour |
| DELETE | `/delete?idTour={uuid}` | Xóa tour |
| PATCH | `/approve?idTour={uuid}` | Phê duyệt tour |

## 5. Service Layer

### TourService.java

| Method | Mô tả |
|--------|--------|
| `getTourDetail(UUID idTour)` | Lấy chi tiết tour |
| `getAllTours()` | Lấy tất cả tour |
| `getListTourApproved()` | Lấy tour đã phê duyệt |
| `getListTourNotApproved()` | Lấy tour chưa phê duyệt |
| `getListTourCreatedByUser(UUID idUser)` | Lấy tour của user |
| `saveTour(TourCreationRequest request)` | Tạo tour mới |
| `updateTour(UUID idTour, TourUpdateRequest request)` | Cập nhật tour |
| `deleteTour(UUID idTour)` | Xóa tour |
| `approveTour(UUID idTour, TourApproveRequest request)` | Phê duyệt tour |

## 6. Repository

### TourRepository.java

```java
findByApprovedTrue();
findByApprovedFalse();
findByUsers_IdUser(UUID idUser);
findAllToursWithSchedules();
findAllSimpleTours();
searchByName(String keyword);
```

## 7. DTOs

### Request DTOs
- `TourCreationRequest` - Tạo tour mới
- `TourUpdateRequest` - Cập nhật tour
- `TourApproveRequest` - Phê duyệt tour

### Response DTOs
- `TourResponse` - Chi tiết tour đầy đủ (kèm schedules, images, owner)
- `TourOverviewResponse` - Thông tin tóm tắt tour
- `TourScheduleResponse` - Chi tiết lịch trình tour
- `OwnerTourResponse` - Thông tin owner của tour

## 8. Related Entities

| Entity | Relationship |
|--------|--------------|
| `TourSchedule` | @OneToMany (mappedBy="tour") |
| `BookingTour` | @OneToMany (mappedBy="tour") |
| `Users` | @ManyToOne |

## 9. File Locations

| Type | Path |
|------|------|
| Entity | `Models/Tour.java` |
| Repository | `Repositories/Jpa/TourRepository.java` |
| Service | `Services/TourService.java` |
| Controller | `Controllers/TourController.java` |
| Constants | `Constant/PredefinedStatusTour.java` |
| | `Constant/PredefinedStatusBookingTour.java` |

## 10. Status Flow

```
Tour Creation:
DRAFT -> APPROVED/REJECTED

Booking Flow:
PENDING -> SUCCESS (payment) -> COMPLETED
PENDING -> FAILED (payment timeout)
```
