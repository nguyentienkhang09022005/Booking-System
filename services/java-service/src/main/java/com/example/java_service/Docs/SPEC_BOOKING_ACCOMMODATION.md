# Booking Accommodation Module Specification

## 1. Overview

Module Booking Accommodation quản lý việc đặt phòng accommodation trong hệ thống BookingNext. Một booking bao gồm thông tin phòng và dịch vụ đi kèm.

## 2. Entities

### Booking.java

```java
@Entity
@Table(name = "booking")
public class Booking {
    UUID idBooking;              // Primary Key
    LocalDateTime bookingDate;    // Ngày đặt (auto-generated)
    LocalDateTime checkinDate;    // Ngày nhận phòng
    LocalDateTime checkoutDate;   // Ngày trả phòng
    String note;                  // Ghi chú
    String status;                // Trạng thái booking
    BigDecimal totalPrice;        // Tổng giá
    Users users;                  // Người đặt (FK -> Users)
    Accommodation accommodation;  // Nơi cư trú (FK -> Accommodation)
}
```

### BookingRoom.java

```java
@Entity
@Table(name = "booking_room")
public class BookingRoom {
    UUID idBookingRoom;           // Primary Key
    BigDecimal priceBookingRoom;  // Giá phòng đã đặt
    Integer quantity;              // Số lượng phòng
    LocalDateTime createAt;        // Ngày tạo
    Booking booking;               // FK -> Booking
    Room room;                     // FK -> Room
}
```

### BookingService.java

```java
@Entity
@Table(name = "booking_service")
public class BookingService {
    UUID idBookingService;              // Primary Key
    Integer quantity;                    // Số lượng
    BigDecimal priceBookingService;      // Giá dịch vụ
    Booking booking;                     // FK -> Booking
    Service service;                     // FK -> Service
}
```

## 3. Constants

### PredefinedStatusBooking.java
```java
STATUS_BOOKING_PENDING    // Chờ thanh toán
STATUS_BOOKING_SUCCESS    // Thanh toán thành công
STATUS_BOOKING_FAILED     // Thanh toán thất bại
STATUS_BOOKING_CHECKED_OUT // Đã checkout
```

## 4. API Endpoints

Base Path: `/booking`

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/getListBooking?idAccommodation={uuid}` | Lấy danh sách booking theo accommodation |
| GET | `/getHistoryListBooking?idUser={uuid}` | Lấy lịch sử booking của user |
| GET | `/getInfBooking?idBooking={uuid}` | Lấy thông tin chi tiết booking |
| POST | `/creation` | Tạo booking mới |
| PATCH | `/update-information?idBooking={uuid}` | Cập nhật thông tin booking |
| PATCH | `/update-status?idBooking={uuid}` | Cập nhật trạng thái booking |
| PATCH | `/checkout?idBooking={uuid}` | Checkout booking |
| PATCH | `/cancel-booking?idBooking={uuid}` | Hủy booking (khi đang PENDING) |
| DELETE | `/delete?idBooking={uuid}` | Xóa booking |

## 5. Service Layer

### BookingHandleService.java (Main Orchestration)

| Method | Mô tả |
|--------|--------|
| `getListBooking(UUID idAccommodation)` | Lấy danh sách booking |
| `getHistoryListBooking(UUID idUser)` | Lấy lịch sử booking user |
| `getInfBooking(UUID idBooking)` | Lấy thông tin chi tiết booking |
| `saveBooking(BookingRequest request)` | Tạo booking + Payment |
| `updateBooking(BookingUpdateRequest request, UUID idBooking)` | Cập nhật booking + Payment |
| `updateStatusBooking(BookingUpdateStatusRequest request, UUID idBooking)` | Cập nhật trạng thái |
| `checkoutBooking(BookingCheckoutRequest request, UUID idBooking)` | Checkout |
| `cancelBookingWhenStatusPending(UUID idBooking)` | Hủy booking PENDING |
| `deleteBookingRoom(UUID idBooking)` | Xóa booking |

### BookingRoomService.java

| Method | Mô tả |
|--------|--------|
| `saveBookingRoom(...)` | Lưu thông tin phòng đặt |

### BookingServiceService.java

| Method | Mô tả |
|--------|--------|
| `saveBookingService(...)` | Lưu thông tin dịch vụ đặt |

## 6. Repository

### BookingRepository.java
```java
findByAccommodation_IdAccommodation(UUID idAccommodation);
findByUsers_IdUser(UUID idUser);
findByAccommodationAndMonthYear(UUID idAccommodation, int month, int year);
```

### BookingRoomRepository.java
```java
findByBooking_IdBooking(UUID idBooking);
```

### BookingServiceRepository.java
```java
findByBooking_IdBooking(UUID idBooking);
```

## 7. DTOs

### Request DTOs
- `BookingRequest` - Tạo booking mới
- `BookingUpdateRequest` - Cập nhật booking
- `BookingUpdateStatusRequest` - Cập nhật trạng thái
- `BookingCheckoutRequest` - Checkout request

### Response DTOs
- `BookingResponse` - Chi tiết booking đầy đủ (rooms, services, vouchers, payment)
- `BookingListResponse` - Thông tin tóm tắt booking
- `BookingRoomResponse` - Chi tiết phòng đặt
- `BookingServiceResponse` - Chi tiết dịch vụ đặt
- `BookingRoomInBookingResponse` - Phòng trong booking
- `BookingServiceInBookingResponse` - Dịch vụ trong booking
- `PaymentResponse` - Thông tin thanh toán

## 8. Booking Flow

```
1. User chọn phòng + dịch vụ
         ↓
2. POST /booking/creation
   - Tạo Booking (status = PENDING)
   - Tạo BookingRoom, BookingService
   - Tạo Payment (status = PENDING)
         ↓
3. User thanh toán qua Payment API
         ↓
4. Payment callback → update status = SUCCESS
         ↓
5a. Nếu thành công → Booking confirmed
         OR
5b. Nếu thất bại/hết hạn → Booking = FAILED
```

## 9. Related Entities

| Entity | Relationship |
|--------|--------------|
| `Users` | @ManyToOne |
| `Accommodation` | @ManyToOne |
| `BookingRoom` | @OneToMany (mappedBy="booking") |
| `BookingService` | @OneToMany (mappedBy="booking") |
| `Payment` | @OneToOne |
| `AppliedVoucher` | @OneToMany |
| `LoyaltyTransaction` | @OneToMany |

## 10. File Locations

| Type | Path |
|------|------|
| Entities | `Models/Booking.java` |
| | `Models/BookingRoom.java` |
| | `Models/BookingService.java` |
| Repositories | `Repositories/Jpa/BookingRepository.java` |
| | `Repositories/Jpa/BookingRoomRepository.java` |
| | `Repositories/Jpa/BookingServiceRepository.java` |
| Services | `Services/BookingHandleService.java` |
| | `Services/BookingRoomService.java` |
| | `Services/BookingServiceService.java` |
| Controller | `Controllers/BookingController.java` |
| Constants | `Constant/PredefinedStatusBooking.java` |

## 11. Price Calculation

```
Total Price = Σ(BookingRoom.priceBookingRoom * quantity) 
            + Σ(BookingService.priceBookingService * quantity)
            - Voucher Discount (nếu có)
```

## 12. Checkout Process

```
1. Xác nhận thông tin checkout (damage, extra services)
2. Tính totalPrice cuối cùng
3. Cập nhật booking.checkoutDate
4. Cập nhật booking.status = CHECKED_OUT
5. Trừ voucher đã sử dụng (nếu có)
```
