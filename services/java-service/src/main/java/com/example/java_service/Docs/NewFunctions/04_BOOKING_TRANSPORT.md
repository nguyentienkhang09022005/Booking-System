# 04 — Đặt vé di chuyển (Booking Transport)

> **Mới ở Đồ án 2.** Đặt vé máy bay/xe khách/tàu, dùng chung hạ tầng thanh toán, voucher và điểm thưởng với đặt phòng/đặt tour.

## 1. Entity `BookingTransport` (bảng `booking_transport`)
| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| idBookingTransport | UUID | Khóa chính |
| quantityTickets | Integer | Số lượng vé |
| priceBookingTransport | BigDecimal | Tổng giá |
| seatNumbers | String | Số ghế (vd `"A1, A2"`), nếu có |
| status | String | `PENDING` / `SUCCESS` / `FAILED` |
| transportTicket / users | FK | Vé được đặt / người đặt |

## 2. API — base path `/booking-transport`
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/creation?paymentMethod=&payerAddress=` | Tạo booking + khởi tạo thanh toán |
| GET | `/detail?idBookingTransport=` | Chi tiết (kèm payment, voucher) |
| GET | `/list-of-user?idUser=` | Danh sách booking của khách |
| GET | `/list-of-business?idUser=` | Danh sách booking thuộc doanh nghiệp |
| PATCH | `/update?idBookingTransport=` | Cập nhật booking + tạo lại payment |
| DELETE | `/cancel?idBookingTransport=` | Hủy booking |

## 3. Tính giá
```
Total = quantityTickets * TransportTicket.price − Voucher discount (nếu có)
```

## 4. Luồng đặt vé
```
Khách chọn vé + số lượng → POST /booking-transport/creation
  → BookingTransport(PENDING) + Payment(PENDING)
  → thanh toán (ZaloPay/Web3) → callback SUCCESS → tích điểm + cộng số dư
  → thất bại/hết hạn → FAILED
```

## 5. Khác biệt so với Booking Tour
| Khía cạnh | BookingTour | BookingTransport |
|-----------|-------------|------------------|
| Số lượng | adult + child | tickets |
| Giá | adultPrice + childPrice | price/vé |
| Trường riêng | pickupLocation, languageSelected, specialRequests | seatNumbers |

## 6. Quan hệ
N—1 `TransportTicket`, N—1 `Users`, 1—1 `Payment`, 1—N `AppliedVoucher`, 1—N `LoyaltyTransaction`.

## 7. Vị trí mã nguồn
`Models/BookingTransport.java`, `Repositories/Jpa/BookingTransportTicketRepository.java`, `Services/BookingTransportTicketService.java`, `Controllers/BookingTransportTicketController.java`, `Constant/PredefinedStatusBookingTransport.java`. Spec gốc: `Docs/SPEC_BOOKING_TRANSPORT_TICKET.md`.
