# Booking Tour Module Specification

## 1. Overview

Module Booking Tour quản lý việc đặt tour du lịch trong hệ thống BookingNext. Booking tour bao gồm thông tin số lượng khách, điểm đón, và ngôn ngữ.

## 2. Entity

### BookingTour.java

```java
@Entity
@Table(name = "booking_tour")
public class BookingTour {
    UUID idBookingTour;              // Primary Key
    Integer quantityAdult;            // Số người lớn (default = 1)
    Integer quantityChild;           // Số trẻ em (default = 0)
    BigDecimal priceBookingTour;      // Giá booking tour
    String pickupLocation;           // Điểm đón khách
    String languageSelected;         // Ngôn ngữ tour
    String specialRequests;          // Yêu cầu đặc biệt
    String status;                   // Trạng thái booking
    Tour tour;                       // Tour được đặt (FK -> Tour)
    Users users;                     // Người đặt (FK -> Users)
    LocalDateTime createdAt;         // Ngày tạo
}
```

## 3. Constants

### PredefinedStatusBookingTour.java
```java
STATUS_BOOKING_PENDING  // Chờ thanh toán
STATUS_BOOKING_SUCCESS  // Thanh toán thành công
STATUS_BOOKING_FAILED   // Thanh toán thất bại
```

## 4. API Endpoints

Base Path: `/booking-tour`

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/creation?paymentMethod={method}&payerAddress={address}` | Tạo booking tour |
| GET | `/detail?idBookingTour={uuid}` | Lấy chi tiết booking tour |
| GET | `/list-of-user?idUser={uuid}` | Lấy danh sách booking tour của user |
| GET | `/list-of-business?idUser={uuid}` | Lấy danh sách booking tour của doanh nghiệp |
| PATCH | `/retry-payment?idBookingTour={uuid}` | Tạo lại thanh toán |
| DELETE | `/cancel?idBookingTour={uuid}` | Hủy booking tour |

## 5. Service Layer

### BookingTourService.java

| Method | Mô tả |
|--------|--------|
| `createBookingTour(BookingTourCreationRequest request, String paymentMethod, String payerAddress)` | Tạo booking tour + payment |
| `createBookingTourTransactional(...)` | Tạo booking trong transaction |
| `callPaymentApi(...)` | Gọi API thanh toán |
| `getBookingTourDetail(UUID idBookingTour)` | Lấy chi tiết booking tour |
| `getListBookingTourByUser(UUID idUser)` | Lấy danh sách booking của user |
| `getListBookingTourByBusiness(UUID idUser)` | Lấy danh sách booking của doanh nghiệp |
| `updateBookingTour(UUID idBookingTour, BookingTourUpdateRequest request)` | Cập nhật booking + tạo lại payment |
| `cancelBookingTour(UUID idBookingTour)` | Hủy booking tour |

## 6. Repository

### BookingTourRepository.java
```java
findByUsers_IdUser(UUID idUser);
findByTour_Users_IdUser(UUID idUser);
findByTourAndMonthYear(Tour tour, int month, int year);
```

## 7. DTOs

### Request DTOs
- `BookingTourCreationRequest` - Tạo booking tour
- `BookingTourUpdateRequest` - Cập nhật booking tour

### Response DTOs
- `BookingTourResponse` - Chi tiết booking tour đầy đủ (kèm payment, vouchers)
- `PaymentResponse` - Thông tin thanh toán

## 8. Booking Flow

```
1. User chọn tour, số lượng khách
         ↓
2. POST /booking-tour/creation
   - Tính giá: (quantityAdult * adultPrice) + (quantityChild * childPrice)
   - Tạo BookingTour (status = PENDING)
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

## 9. Price Calculation

```
Total Price = (quantityAdult * Tour.adultPrice) 
            + (quantityChild * Tour.childPrice)
            - Voucher Discount (nếu có)
```

## 10. Related Entities

| Entity | Relationship |
|--------|--------------|
| `Tour` | @ManyToOne |
| `Users` | @ManyToOne |
| `Payment` | @OneToOne |
| `AppliedVoucher` | @OneToMany |
| `LoyaltyTransaction` | @OneToMany |

## 11. File Locations

| Type | Path |
|------|------|
| Entity | `Models/BookingTour.java` |
| Repository | `Repositories/Jpa/BookingTourRepository.java` |
| Service | `Services/BookingTourService.java` |
| Controller | `Controllers/BookingTourController.java` |
| Constants | `Constant/PredefinedStatusBookingTour.java` |

## 12. Cancellation Policy

Khi hủy booking, áp dụng chính sách hủy của tour (`Tour.cancellationPolicy`):
- Hủy trước X ngày: hoàn tiền Y%
- Hủy sau thời gian này: không hoàn tiền

(X và Y được định nghĩa trong cancellationPolicy của tour)
