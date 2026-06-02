# BookingNext — Tổng quan cập nhật Đồ án 2 so với Đồ án 1

> Tài liệu mô tả các chức năng **mới** được bổ sung trong Đồ án 2, đặt cạnh phạm vi của Đồ án 1.
> Mỗi nhóm chức năng có một file chi tiết riêng trong thư mục `Docs/NewFunctions`.

---

## 1. Bối cảnh

**Đồ án 1 (BookingNext v1)** là nền tảng đặt phòng khách sạn (OTA) với phạm vi:

- Quản lý khách sạn / phòng / dịch vụ, đặt phòng (booking accommodation) chống overbooking.
- Thanh toán **ZaloPay**, báo cáo doanh thu (Golang), thông báo real-time (Node.js + Socket.IO).
- Chat AI **cơ bản** (Gemini qua Node.js) tư vấn + kiểm duyệt review.
- Voucher đơn giản, đánh giá, xác thực JWT/OAuth2, kiến trúc microservices.

**Đồ án 2 (BookingNext v2)** mở rộng từ "đặt phòng" thành **hệ sinh thái du lịch trọn gói**: thêm sản phẩm tour & vé di chuyển, AI thế hệ mới (RAG + lập lịch trình tự động), tài chính số trên blockchain, các công cụ tăng tương tác (bản tin, sổ tay, điểm thưởng) và phân tích kinh doanh.

---

## 2. Bảng so sánh tổng hợp

| Lĩnh vực | Đồ án 1 | Đồ án 2 (mới / mở rộng) |
|----------|---------|--------------------------|
| Sản phẩm | Khách sạn / phòng / dịch vụ | **+ Tour**, **+ Vé di chuyển** (máy bay/xe khách/tàu) |
| Đặt chỗ | Booking phòng | **+ Booking Tour**, **+ Booking Vé di chuyển** |
| Voucher | Voucher đơn giản | **Phân tầng**: System vs Business, `applyTo` (ROOM/SERVICE/TOUR/TICKET/ALL), min/max discount |
| Thanh toán | ZaloPay | **+ Blockchain (VND stablecoin ERC-20, EIP-712 gasless)** + ví số dư doanh nghiệp + rút tiền |
| AI Chat | Gemini cơ bản (Node.js) | **Chatbot RAG** trong java-service: Groq Llama-4-Scout + Gemini embedding + PgVector + 32 tools + Redis memory |
| AI khác | — | **Tạo lịch trình tự động** (AI Scheduler, pipeline 2 LLM) theo sở thích người dùng |
| Tìm kiếm | Filter cơ bản | **Bộ Filter + Search mới**: fuzzy matching (Jaro-Winkler/Levenshtein), xử lý dấu tiếng Việt, re-rank theo độ liên quan |
| Tương tác | — | **Bản tin** (bulletin board), **Sổ tay cá nhân** (notebook), **Điểm thưởng** (loyalty) |
| Phân tích | Báo cáo doanh thu | **+ Business Analytics**: phân khúc khách hàng (RFM) + insight bằng AI |
| Cấu hình | Hardcode | **Tham số hệ thống** (system parameter) chỉnh runtime |

---

## 3. Danh sách nhóm chức năng mới & file chi tiết

| # | Nhóm chức năng | File | Base path API |
|---|----------------|------|----------------|
| 1 | Tour du lịch | `01_TOUR.md` | `/tour` |
| 2 | Vé di chuyển | `02_TRANSPORT_TICKET.md` | `/transport-ticket` |
| 3 | Đặt tour | `03_BOOKING_TOUR.md` | `/booking-tour` |
| 4 | Đặt vé di chuyển | `04_BOOKING_TRANSPORT.md` | `/booking-transport` |
| 5 | Phân tầng voucher | `05_VOUCHER_TIERING.md` | `/voucher` |
| 6 | Bản tin | `06_BULLETIN_BOARD.md` | `/bulletin` |
| 7 | Tạo lịch trình AI | `07_ITINERARY_AI.md` | `/itinerary`, `/ai-scheduler` |
| 8 | Chatbot AI (RAG) | `08_CHATBOT_RAG.md` | `/chatbot` |
| 9 | Sổ tay cá nhân | `09_NOTEBOOK.md` | `/notebook` |
| 10 | Điểm thưởng | `10_LOYALTY.md` | `/loyalty` |
| 11 | Thanh toán blockchain | `11_BLOCKCHAIN_PAYMENT.md` | `/web3/payment`, `/internal`, `/withdrawal` |
| 12 | Tham số hệ thống | `12_SYSTEM_PARAMETERS.md` | `/system-parameter` |
| 13 | Business Analytics | `13_BUSINESS_ANALYTICS.md` | `/analytics` |
| 14 | Filter & Search mới | `14_FILTER_SEARCH.md` | `/item-filter`, `/item-search` |
| 15 | Phân khúc khách hàng (AI) | `15_CUSTOMER_SEGMENTATION.md` | (engine nền, dữ liệu cho `/analytics`) |
| 16 | Ví số dư & Rút tiền doanh nghiệp | `16_COMPANY_BALANCE_WITHDRAWAL.md` | `/company`, `/admin/withdrawal`, `/internal` |

> **Phụ lục — có spec nhưng CHƯA triển khai (không nằm trong danh sách trên):**
> `Docs/FEATURE_03_CALENDAR_SYNC.md` (đồng bộ Google/Apple Calendar) và `Docs/FEATURE_04_GROUP_TRAVEL.md`
> (du lịch nhóm & chia tiền) hiện chỉ là tài liệu thiết kế — chưa có Model/Service/Controller trong mã nguồn.

---

## 4. Tác động kiến trúc

- **java-service** (Spring Boot): chứa phần lớn nghiệp vụ mới (tour, vé, booking, voucher, loyalty, bulletin, notebook, itinerary/AI, chatbot RAG, analytics, filter/search, ví số dư).
- **golang-service** (Gin): mở rộng thành cổng thanh toán đa kênh — ZaloPay **và** Web3; sau khi thanh toán thành công gọi nội bộ `POST /internal/payment-success` sang java-service để cộng số dư ví doanh nghiệp + tích điểm.
- **contracts** (Solidity/Foundry): hai smart contract `VNDToken` (ERC-20 stablecoin) và `PaymentGateway` triển khai trên **Sepolia testnet**.
- **PostgreSQL**: bổ sung bảng cho tour, transport_ticket, booking_tour, booking_transport, voucher (tiering), loyalty_transaction, bulletin_board, notebook/note/note_block, itinerary/itinerary_item, user_preferences, company_balance/withdrawal_request, system_parameter, knowledge base vector (PgVector).
- **MongoDB**: lưu ảnh (images collection) cho bulletin & notebook; vẫn dùng cho chat/log/review như cũ.
- **Redis**: thêm bộ nhớ hội thoại cho Chatbot RAG (ngoài cache & token sẵn có).

---

## 5. Định hướng đọc tài liệu

- Người đọc nghiệp vụ: bắt đầu từ file này → các file 01–14 theo nhóm quan tâm.
- Người đọc kỹ thuật: mỗi file chi tiết liệt kê Entity, Endpoint, Service, luồng xử lý và vị trí file mã nguồn.
- Tài liệu kỹ thuật chuyên sâu sẵn có (tham chiếu): `RAG_ARCHITECTURE.md`, `SPEC_CHATBOT_AI.md`, `SPEC_AI_SCHEDULE.md`, `ITEM_FILTER_SYSTEM.md`, `CUSTOMER_SEGMENTATION.md`, `WEB3_PAYMENT_ECOSYSTEM.md`, `golang-service/BUSINESS_FLOW_BALANCE.md`.
