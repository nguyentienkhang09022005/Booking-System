# Booking Filter Module Specification

## 1. Overview

Module Booking Filter cung cấp các API để lọc và thống kê bookings theo tháng/năm cho admin và doanh nghiệp. Hỗ trợ 3 loại: Accommodation, Tour, Transport.

## 2. Service Layer

### BookingFilterService.java

| Method | Mô tả |
|--------|--------|
| `getSimpleAccommodations()` | Lấy danh sách accommodation đơn giản (id + name) |
| `getSimpleTours()` | Lấy danh sách tour đơn giản (id + name) |
| `getSimpleTransportTickets()` | Lấy danh sách transport ticket đơn giản (id + name) |
| `filterAccommodationBookings(UUID idAccommodation, int month, int year)` | Lọc booking phòng theo tháng |
| `filterTourBookings(UUID idTour, int month, int year)` | Lọc booking tour theo tháng |
| `filterTransportBookings(UUID idTicket, int month, int year)` | Lọc booking vé theo tháng |

## 3. API Endpoints

Base Path: `/filter`

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/accommodations` | Lấy danh sách nơi cư trú |
| GET | `/tours` | Lấy danh sách tour |
| GET | `/transport-tickets` | Lấy danh sách vé di chuyển |
| GET | `/bookings/accommodation?idAccommodation={uuid}&month={m}&year={y}` | Lọc đặt phòng theo tháng |
| GET | `/bookings/tour?idTour={uuid}&month={m}&year={y}` | Lọc đặt tour theo tháng |
| GET | `/bookings/transport?idTicket={uuid}&month={m}&year={y}` | Lọc đặt vé theo tháng |

## 4. DTOs

### SimpleItemResponse
```java
UUID id;        // ID của item
String name;    // Tên item
```

### BookingFilterResponse<T>
```java
List<T> bookings;                    // Danh sách bookings
VoucherStatisticResponse statistics; // Thống kê voucher
```

### VoucherStatisticResponse
```java
int totalVouchersUsed;      // Tổng voucher đã sử dụng
BigDecimal totalDiscount;    // Tổng giảm giá
```

## 5. Filter by Month/Year

### Accommodation Bookings
```
Input: idAccommodation, month, year
Logic: 
  - Find all Booking where accommodation.id = idAccommodation
  - AND bookingDate in (month, year)
Output: List<BookingListResponse> + VoucherStatistics
```

### Tour Bookings
```
Input: idTour, month, year
Logic:
  - Find all BookingTour where tour.id = idTour
  - AND createdAt in (month, year)
Output: List<BookingTourResponse> + VoucherStatistics
```

### Transport Bookings
```
Input: idTicket, month, year
Logic:
  - Find all BookingTransport where transportTicket.id = idTicket
  - AND createdAt in (month, year)
Output: List<BookingTransportResponse> + VoucherStatistics
```

## 6. Voucher Statistics Calculation

```java
VoucherStatisticResponse:
  totalVouchersUsed = COUNT(AppliedVoucher where booking matches filter)
  totalDiscount = SUM(AppliedVoucher.discountAmount)
```

## 7. Use Cases

### Admin Dashboard
- Xem tổng quan bookings trong tháng
- Thống kê doanh thu và voucher sử dụng

### Business Owner
- Xem bookings của accommodation/tour/transport mình
- Filter theo tháng để so sánh

## 8. Controller

### FilterController.java

```java
BookingFilterService bookingFilterService;

// Lấy danh sách items đơn giản (cho dropdown filter)
GET /accommodations
GET /tours  
GET /transport-tickets

// Filter bookings theo tháng/năm
GET /bookings/accommodation?idAccommodation={uuid}&month={m}&year={y}
GET /bookings/tour?idTour={uuid}&month={m}&year={y}
GET /bookings/transport?idTicket={uuid}&month={m}&year={y}
```

## 9. File Locations

| Type | Path |
|------|------|
| Service | `Services/BookingFilterService.java` |
| Controller | `Controllers/FilterController.java` |

## 10. Example Response

### GET /filter/bookings/accommodation?idAccommodation=uuid&month=4&year=2026

```json
{
  "message": "Lọc danh sách đặt phòng tháng 4/2026 thành công!",
  "data": {
    "bookings": [
      {
        "idBooking": "uuid",
        "bookingDate": "2026-04-15T10:30:00",
        "checkinDate": "2026-04-20T14:00:00",
        "checkoutDate": "2026-04-22T12:00:00",
        "totalPrice": 2500000,
        "status": "SUCCESS",
        "userName": "Nguyễn Văn A"
      }
    ],
    "statistics": {
      "totalVouchersUsed": 5,
      "totalDiscount": 750000
    }
  }
}
```
