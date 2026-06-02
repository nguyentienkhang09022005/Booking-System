# 03 — Đặt tour (Booking Tour)

> **Mới ở Đồ án 2.** Mở rộng nghiệp vụ đặt chỗ từ "đặt phòng" sang **đặt tour**, tái sử dụng cùng hạ tầng thanh toán (ZaloPay/Web3), voucher, điểm thưởng.

## 1. Entity `BookingTour` (bảng `booking_tour`)
| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| idBookingTour | UUID | Khóa chính |
| quantityAdult / quantityChild | Integer | Số người lớn (mặc định 1) / trẻ em (mặc định 0) |
| priceBookingTour | BigDecimal | Tổng giá |
| pickupLocation | String | Điểm đón |
| languageSelected | String | Ngôn ngữ tour |
| specialRequests | String | Yêu cầu đặc biệt |
| status | String | `PENDING` / `SUCCESS` / `FAILED` |
| tour / users | FK | Tour được đặt / người đặt |

## 2. API — base path `/booking-tour`
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/creation?paymentMethod=&payerAddress=` | Tạo booking + khởi tạo thanh toán |
| GET | `/detail?idBookingTour=` | Chi tiết (kèm payment, voucher) |
| GET | `/list-of-user?idUser=` | Danh sách booking của khách |
| GET | `/list-of-business?idUser=` | Danh sách booking thuộc doanh nghiệp |
| PATCH | `/retry-payment?idBookingTour=` | Tạo lại thanh toán |
| DELETE | `/cancel?idBookingTour=` | Hủy booking |

> `paymentMethod` chọn kênh thanh toán (ZaloPay hoặc Web3 VND stablecoin); `payerAddress` là địa chỉ ví khi thanh toán blockchain (xem `11_BLOCKCHAIN_PAYMENT.md`).

## 3. Tính giá
```
Total = quantityAdult * Tour.adultPrice + quantityChild * Tour.childPrice − Voucher discount (nếu có)
```

## 4. Luồng đặt tour
```
1. Khách chọn tour + số khách
2. POST /booking-tour/creation → tạo BookingTour(PENDING) + Payment(PENDING)
3. Khách thanh toán qua golang-service (ZaloPay/Web3)
4. Callback thành công → status = SUCCESS → tích điểm + cộng số dư doanh nghiệp
5. Thất bại/hết hạn → status = FAILED
```

## 5. Quan hệ
N—1 `Tour`, N—1 `Users`, 1—1 `Payment`, 1—N `AppliedVoucher`, 1—N `LoyaltyTransaction`. Áp dụng `Tour.cancellationPolicy` khi hủy.

## 6. Vị trí mã nguồn
`Models/BookingTour.java`, `Repositories/Jpa/BookingTourRepository.java`, `Services/BookingTourService.java`, `Controllers/BookingTourController.java`, `Constant/PredefinedStatusBookingTour.java`. Spec gốc: `Docs/SPEC_BOOKING_TOUR.md`.
