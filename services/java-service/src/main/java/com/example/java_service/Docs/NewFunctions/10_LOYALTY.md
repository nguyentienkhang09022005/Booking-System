# 10 — Điểm thưởng (Loyalty)

> **Mới ở Đồ án 2.** Hệ thống tích điểm khi khách thanh toán thành công bất kỳ loại đặt chỗ nào (phòng / tour / vé), tạo nền tảng cho chương trình khách hàng thân thiết.

## 1. Entity `LoyaltyTransaction` (bảng `loyalty_transaction`)
| Trường | Ý nghĩa |
|--------|---------|
| idLoyaltyTrans | Khóa chính |
| points | Số điểm tích |
| description | Mô tả giao dịch |
| users | Người dùng (FK) |
| booking / bookingTour / bookingTransport | Nguồn phát sinh (1 trong 3, còn lại null) |
| createdAt | Thời điểm |

## 2. Công thức tích điểm
```
points = floor(TotalBookingPrice / VND_PER_POINT)   // mặc định VND_PER_POINT = 1000
```
Ví dụ: booking 2.850.000đ → 2850 điểm. Tỷ lệ `VND_PER_POINT` cấu hình qua **Tham số hệ thống** (`12_SYSTEM_PARAMETERS.md`).

## 3. Khi nào được tích
Khi booking đạt trạng thái `SUCCESS` (thanh toán thành công). Trong luồng thực tế, **golang-service** sau khi xác nhận thanh toán (ZaloPay/Web3) gọi `POST /loyalty/add-points` để tích điểm tương ứng nguồn booking.

## 4. API — base path `/loyalty`
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/add-points` | Thêm điểm (gọi nội bộ sau thanh toán) |
| GET | `/my-history?idUser=` | Toàn bộ lịch sử tích điểm |
| GET | `/booking-room?idUser=` | Lịch sử từ đặt phòng |
| GET | `/booking-tour?idUser=` | Lịch sử từ đặt tour |
| GET | `/booking-transport?idUser=` | Lịch sử từ đặt vé |

## 5. Mục đích sử dụng điểm
Đổi voucher giảm giá / ưu đãi đặc biệt (có thể mở rộng). Lịch sử điểm cũng được Chatbot RAG truy vấn qua tool cá nhân `getLoyaltyHistory`.

## 6. Vị trí mã nguồn
`Models/LoyaltyTransaction.java`, `Repositories/Jpa/LoyaltyTransactionRepository.java`, `Services/LoyaltyTransactionService.java`, `Controllers/LoyaltyTransactionController.java`, `Constant/PredefinedLoyaltyPoint.java`. Spec gốc: `Docs/SPEC_LOYALTY_TRANSACTION.md`.
