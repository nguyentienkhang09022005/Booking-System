# Loyalty Transaction Module Specification

## 1. Overview

Module Loyalty Transaction quản lý hệ thống tích điểm loyalty cho người dùng. Mỗi khi đặt phòng, đặt tour, hoặc đặt vé di chuyển, người dùng sẽ được tích điểm dựa trên tổng giá trị booking.

## 2. Entity

### LoyaltyTransaction.java

```java
@Entity
@Table(name = "loyalty_transaction")
public class LoyaltyTransaction {
    UUID idLoyaltyTrans;              // Primary Key
    Integer points;                   // Số điểm tích/lưu
    String description;              // Mô tả giao dịch
    Users users;                     // Người dùng (FK -> Users)
    Booking booking;                 // Booking phòng (FK -> Booking, nullable)
    BookingTour bookingTour;         // Booking tour (FK -> BookingTour, nullable)
    BookingTransport bookingTransport; // Booking vé (FK -> BookingTransport, nullable)
    LocalDateTime createdAt;         // Ngày tạo
}
```

## 3. Constants

### PredefinedLoyaltyPoint.java
```java
DEFAULT_VND_PER_POINT  // 1000 VND = 1 điểm (configurable)
```

## 4. API Endpoints

Base Path: `/loyalty`

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/add-points` | Thêm điểm loyalty |
| GET | `/my-history?idUser={uuid}` | Lấy tất cả lịch sử tích điểm |
| GET | `/booking-room?idUser={uuid}` | Lấy lịch sử tích điểm từ đặt phòng |
| GET | `/booking-tour?idUser={uuid}` | Lấy lịch sử tích điểm từ đặt tour |
| GET | `/booking-transport?idUser={uuid}` | Lấy lịch sử tích điểm từ đặt vé |

## 5. Service Layer

### LoyaltyTransactionService.java

| Method | Mô tả |
|--------|--------|
| `processLoyaltyPoints(AddPointRequest request)` | Xử lý tích điểm |
| `getHistoryByUser(UUID idUser)` | Lấy tất cả lịch sử |
| `getRoomBookingHistoryByUser(UUID idUser)` | Lịch sử đặt phòng |
| `getTourBookingHistoryByUser(UUID idUser)` | Lịch sử đặt tour |
| `getTransportBookingHistoryByUser(UUID idUser)` | Lịch sử đặt vé |

## 6. Repository

### LoyaltyTransactionRepository.java
```java
findByUsers_IdUserOrderByCreatedAtDesc(UUID idUser);
findByUsers_IdUserAndBookingIsNotNullOrderByCreatedAtDesc(UUID idUser);
findByUsers_IdUserAndBookingTourIsNotNullOrderByCreatedAtDesc(UUID idUser);
findByUsers_IdUserAndBookingTransportIsNotNullOrderByCreatedAtDesc(UUID idUser);
```

## 7. DTOs

### Request DTOs
- `AddPointRequest` - Request thêm điểm

### Response DTOs
- `LoyaltyTransactionResponse` - Chi tiết giao dịch loyalty

## 8. Points Calculation

```
Points = floor(TotalBookingPrice / VND_PER_POINT)

Ví dụ: 
- VND_PER_POINT = 1000
- TotalBookingPrice = 2,850,000 VND
- Points = floor(2,850,000 / 1000) = 2850 điểm
```

## 9. When Points Are Awarded

Điểm được tích khi booking có trạng thái thành công (`STATUS_BOOKING_SUCCESS`):

| Booking Type | Trigger |
|-------------|---------|
| Booking (Accommodation) | Thanh toán phòng thành công |
| BookingTour | Thanh toán tour thành công |
| BookingTransport | Thanh toán vé thành công |

## 10. Points Usage

Điểm loyalty có thể được sử dụng để:
- Đổi voucher giảm giá
- Đổi các ưu đãi đặc biệt
- (Có thể mở rộng thêm trong tương lai)

## 11. Transaction Description Format

```java
// Booking Accommodation
description = "Tích điểm từ đặt phòng: [AccommodationName] - [BookingID]"

// Booking Tour  
description = "Tích điểm từ đặt tour: [TourName] - [BookingTourID]"

// Booking Transport
description = "Tích điểm từ đặt vé: [ProviderName] - [BookingTransportID]"
```

## 12. Related Entities

| Entity | Relationship |
|--------|--------------|
| `Users` | @ManyToOne |
| `Booking` | @ManyToOne (nullable) |
| `BookingTour` | @ManyToOne (nullable) |
| `BookingTransport` | @ManyToOne (nullable) |

## 13. File Locations

| Type | Path |
|------|------|
| Entity | `Models/LoyaltyTransaction.java` |
| Repository | `Repositories/Jpa/LoyaltyTransactionRepository.java` |
| Service | `Services/LoyaltyTransactionService.java` |
| Controller | `Controllers/LoyaltyTransactionController.java` |
| Constants | `Constant/PredefinedLoyaltyPoint.java` |

## 14. Example Flow

```
1. User đặt phòng 2,500,000 VND
         ↓
2. Thanh toán thành công
         ↓
3. BookingHandleService gọi LoyaltyTransactionService.processLoyaltyPoints()
         ↓
4. Tạo LoyaltyTransaction:
   - points = 2500 (2500000 / 1000)
   - booking = idBooking
   - bookingTour = null
   - bookingTransport = null
   - description = "Tích điểm từ đặt phòng: Grand Hotel - uuid..."
```
