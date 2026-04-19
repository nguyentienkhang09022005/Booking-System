# Booking Transport Ticket Module Specification

## 1. Overview

Module Booking Transport Ticket quản lý việc đặt vé di chuyển (máy bay, xe khách, tàu hỏa, tàu cao tốc) trong hệ thống BookingNext.

## 2. Entity

### BookingTransport.java

```java
@Entity
@Table(name = "booking_transport")
public class BookingTransport {
    UUID idBookingTransport;           // Primary Key
    Integer quantityTickets;           // Số lượng vé
    BigDecimal priceBookingTransport;  // Giá booking
    String seatNumbers;                // Số ghế (nếu có)
    String status;                     // Trạng thái booking
    TransportTicket transportTicket;   // Vé di chuyển (FK -> TransportTicket)
    Users users;                       // Người đặt (FK -> Users)
    LocalDateTime createdAt;           // Ngày tạo
}
```

## 3. Constants

### PredefinedStatusBookingTransport.java
```java
STATUS_BOOKING_PENDING  // Chờ thanh toán
STATUS_BOOKING_SUCCESS  // Thanh toán thành công
STATUS_BOOKING_FAILED   // Thanh toán thất bại
```

## 4. API Endpoints

Base Path: `/booking-transport`

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/creation?paymentMethod={method}&payerAddress={address}` | Tạo booking vé |
| GET | `/detail?idBookingTransport={uuid}` | Lấy chi tiết booking vé |
| GET | `/list-of-user?idUser={uuid}` | Lấy danh sách booking vé của user |
| GET | `/list-of-business?idUser={uuid}` | Lấy danh sách booking vé của doanh nghiệp |
| PATCH | `/update?idBookingTransport={uuid}` | Cập nhật booking vé |
| DELETE | `/cancel?idBookingTransport={uuid}` | Hủy booking vé |

## 5. Service Layer

### BookingTransportTicketService.java

| Method | Mô tả |
|--------|--------|
| `createBookingTransport(BookingTransportCreationRequest request, String paymentMethod, String payerAddress)` | Tạo booking vé + payment |
| `createBookingTransportTransactional(...)` | Tạo booking trong transaction |
| `callPaymentApi(...)` | Gọi API thanh toán |
| `getBookingDetail(UUID idBookingTransport)` | Lấy chi tiết booking vé |
| `getListBookingByUser(UUID idUser)` | Lấy danh sách booking của user |
| `getListBookingByBusiness(UUID idUser)` | Lấy danh sách booking của doanh nghiệp |
| `updateBookingTransport(UUID idBookingTransport, BookingTransportUpdateRequest request)` | Cập nhật booking + tạo lại payment |
| `cancelBookingTransport(UUID idBookingTransport)` | Hủy booking vé |

## 6. Repository

### BookingTransportTicketRepository.java
```java
findByUsers_IdUser(UUID idUser);
findByTransportTicket_Users_IdUser(UUID idUser);
findByTransportTicketAndMonthYear(TransportTicket ticket, int month, int year);
```

## 7. DTOs

### Request DTOs
- `BookingTransportCreationRequest` - Tạo booking vé
- `BookingTransportUpdateRequest` - Cập nhật booking vé

### Response DTOs
- `BookingTransportResponse` - Chi tiết booking vé đầy đủ (kèm payment, vouchers)
- `PaymentResponse` - Thông tin thanh toán

## 8. Booking Flow

```
1. User chọn vé, số lượng
         ↓
2. POST /booking-transport/creation
   - Tính giá: quantityTickets * TransportTicket.price
   - Tạo BookingTransport (status = PENDING)
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
Total Price = quantityTickets * TransportTicket.price
            - Voucher Discount (nếu có)
```

## 10. Seat Numbers

Đối với xe khách hoặc tàu, `seatNumbers` lưu thông tin số ghế đã chọn:
- Format: "A1, A2, A3" (nhiều ghế)
- hoặc "A1" (1 ghế)

## 11. Related Entities

| Entity | Relationship |
|--------|--------------|
| `TransportTicket` | @ManyToOne |
| `Users` | @ManyToOne |
| `Payment` | @OneToOne |
| `AppliedVoucher` | @OneToMany |
| `LoyaltyTransaction` | @OneToMany |

## 12. File Locations

| Type | Path |
|------|------|
| Entity | `Models/BookingTransport.java` |
| Repository | `Repositories/Jpa/BookingTransportTicketRepository.java` |
| Service | `Services/BookingTransportTicketService.java` |
| Controller | `Controllers/BookingTransportTicketController.java` |
| Constants | `Constant/PredefinedStatusBookingTransport.java` |

## 13. Difference from BookingTour

| Aspect | BookingTour | BookingTransport |
|--------|-------------|------------------|
| Quantity | adult + child | tickets only |
| Price | adultPrice + childPrice | ticket price |
| Special Fields | pickupLocation, languageSelected, specialRequests | seatNumbers |
