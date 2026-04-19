# Transport Ticket Module Specification

## 1. Overview

Module Transport Ticket quản lý các vé di chuyển (máy bay, xe khách, tàu hỏa, tàu cao tốc) trong hệ thống BookingNext.

## 2. Entity

### TransportTicket.java

```java
@Entity
@Table(name = "transport_ticket")
public class TransportTicket {
    UUID idTicket;              // Primary Key
    Users users;                // Công ty cung cấp vé (FK -> Users)
    String ticketType;          // Loại vé: Plane, Coach, Train, PassengerShip
    String ticketClass;         // Hạng vé: Economy, Business, VIP
    String providerName;        // Tên nhà cung cấp
    String departureLocation;   // Điểm khởi hành
    String arrivalLocation;     // Điểm đến
    LocalDateTime departureTime; // Giờ khởi hành
    LocalDateTime arrivalTime;   // Giờ đến
    BigDecimal price;           // Giá vé
    String status;              // Trạng thái vé
    Boolean approved;           // Đã phê duyệt
    LocalDateTime createdAt;    // Ngày tạo
}
```

## 3. Constants

### PredefinedTypeTransportTicket.java
```java
COACH_TICKET         // Vé xe khách
PLANE_TICKET        // Vé máy bay
TRAIN_TICKET        // Vé tàu hỏa
PASSENGER_SHIP_TICKET // Vé tàu cao tốc/thuyền
```

### PredefinedStatusTransportTicket.java
```java
STATUS_TRANSPORT_TICKET_AVAILABLE  // Còn vé
STATUS_TRANSPORT_TICKET_SOLD_OUT   // Hết vé
```

### PredefinedStatusBookingTransport.java
```java
STATUS_BOOKING_PENDING  // Chờ thanh toán
STATUS_BOOKING_SUCCESS  // Thanh toán thành công
STATUS_BOOKING_FAILED   // Thanh toán thất bại
```

## 4. API Endpoints

Base Path: `/transport-ticket`

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/detail?idTicket={uuid}` | Lấy chi tiết vé |
| GET | `/get-all` | Lấy tất cả vé |
| GET | `/get-list-approved` | Lấy vé đã phê duyệt |
| GET | `/get-list-not-approved` | Lấy vé chưa phê duyệt |
| GET | `/list-of-user?idUser={uuid}` | Lấy vé của người dùng |
| POST | `/creation` | Tạo vé mới |
| PATCH | `/update?idTicket={uuid}` | Cập nhật vé |
| DELETE | `/delete?idTicket={uuid}` | Xóa vé |
| PATCH | `/approve?idTicket={uuid}` | Phê duyệt vé |

## 5. Service Layer

### TransportTicketService.java

| Method | Mô tả |
|--------|--------|
| `getTransportTicketDetail(UUID idTicket)` | Lấy chi tiết vé |
| `getAllTickets()` | Lấy tất cả vé |
| `getListTicketApproved()` | Lấy vé đã phê duyệt |
| `getListTicketNotApproved()` | Lấy vé chưa phê duyệt |
| `getListTicketCreatedByUser(UUID idUser)` | Lấy vé của user |
| `saveTransportTicket(TransportTicketCreationRequest request)` | Tạo vé mới |
| `updateTransportTicket(UUID idTicket, TransportTicketUpdateRequest request)` | Cập nhật vé |
| `deleteTransportTicket(UUID idTicket)` | Xóa vé |
| `approveTransportTicket(UUID idTicket, TransportTicketApproveRequest request)` | Phê duyệt vé |

## 6. Repository

### TransportTicketRepository.java

```java
findByApprovedTrue();
findByApprovedFalse();
findByUsers_IdUser(UUID idUser);
findAllSimpleTransportTickets();
searchByProviderName(String providerName);
searchByDepartureLocation(String location);
searchByArrivalLocation(String location);
searchByTicketType(String ticketType);
```

## 7. DTOs

### Request DTOs
- `TransportTicketCreationRequest` - Tạo vé mới
- `TransportTicketUpdateRequest` - Cập nhật vé
- `TransportTicketApproveRequest` - Phê duyệt vé

### Response DTOs
- `TransportTicketResponse` - Chi tiết vé đầy đủ (kèm owner)
- `TransportTicketOverviewResponse` - Thông tin tóm tắt vé
- `OwnerTransportTicketResponse` - Thông tin owner của vé

## 8. Related Entities

| Entity | Relationship |
|--------|--------------|
| `BookingTransport` | @OneToMany (mappedBy="transportTicket") |
| `Users` | @ManyToOne |

## 9. File Locations

| Type | Path |
|------|------|
| Entity | `Models/TransportTicket.java` |
| Repository | `Repositories/Jpa/TransportTicketRepository.java` |
| Service | `Services/TransportTicketService.java` |
| Controller | `Controllers/TransportTicketController.java` |
| Constants | `Constant/PredefinedTypeTransportTicket.java` |
| | `Constant/PredefinedStatusTransportTicket.java` |
| | `Constant/PredefinedStatusBookingTransport.java` |

## 10. Ticket Type Keywords (AI Scheduler)

```
 máy bay, plane, airplane -> Plane
 xe khách, coach, bus -> Coach
 tàu hỏa, train -> Train
 tàu cao tốc, tàu thuyền -> PassengerShip
```

## 11. Booking Flow

```
Booking Creation:
PENDING -> SUCCESS (payment) -> COMPLETED
PENDING -> FAILED (payment timeout)
```
