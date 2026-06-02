# 02 — Vé di chuyển (Transport Ticket)

> **Mới ở Đồ án 2.** Bổ sung sản phẩm **vé di chuyển**: máy bay, xe khách, tàu hỏa, tàu cao tốc/thuyền — giúp BookingNext phục vụ trọn chuyến đi (di chuyển + lưu trú + tour).

## 1. Mục đích
Doanh nghiệp đăng bán vé theo tuyến (điểm đi → điểm đến) với giờ khởi hành/đến và giá; khách đặt vé. Admin phê duyệt trước khi hiển thị.

## 2. Entity `TransportTicket` (bảng `transport_ticket`)
| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| idTicket | UUID | Khóa chính |
| users | Users (FK) | Doanh nghiệp cung cấp vé |
| ticketType | String | `Plane` / `Coach` / `Train` / `PassengerShip` |
| ticketClass | String | `Economy` / `Business` / `VIP` |
| providerName | String | Nhà cung cấp |
| departureLocation / arrivalLocation | String | Điểm đi / điểm đến |
| departureTime / arrivalTime | LocalDateTime | Giờ khởi hành / đến |
| price | BigDecimal | Giá vé |
| status | String | `Available` / `Sold Out` |
| approved | Boolean | Đã phê duyệt |

## 3. API — base path `/transport-ticket`
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/detail?idTicket=` | Chi tiết vé (kèm owner) |
| GET | `/get-all` | Tất cả vé |
| GET | `/get-list-approved` | Vé đã duyệt (công khai) |
| GET | `/get-list-not-approved` | Vé chờ duyệt (Admin) |
| GET | `/list-of-user?idUser=` | Vé của một doanh nghiệp |
| POST | `/creation` | Tạo vé |
| PATCH | `/update?idTicket=` | Cập nhật vé |
| DELETE | `/delete?idTicket=` | Xóa vé |
| PATCH | `/approve?idTicket=` | Admin phê duyệt |

## 4. Service / Repository
- `TransportTicketService`: CRUD + `getListTicketApproved/NotApproved`, `getListTicketCreatedByUser`, `approveTransportTicket`.
- `TransportTicketRepository`: `findByApprovedTrue/False`, `findByUsers_IdUser`, `searchByProviderName/DepartureLocation/ArrivalLocation/TicketType`.

## 5. Từ khóa loại vé (dùng cho AI Scheduler / Chatbot)
```
máy bay, plane, airplane → Plane
xe khách, coach, bus      → Coach
tàu hỏa, train            → Train
tàu cao tốc, tàu thuyền   → PassengerShip
```

## 6. Quan hệ
`TransportTicket` 1—N `BookingTransport`, N—1 `Users`. Là đối tượng được hỗ trợ bởi Filter/Search mới và được AI Scheduler tra cứu khi cần chặng di chuyển.

## 7. Vị trí mã nguồn
`Models/TransportTicket.java`, `Repositories/Jpa/TransportTicketRepository.java`, `Services/TransportTicketService.java`, `Controllers/TransportTicketController.java`, `Constant/PredefinedTypeTransportTicket.java`, `PredefinedStatusTransportTicket.java`. Spec gốc: `Docs/SPEC_TRANSPORT_TICKET.md`.
