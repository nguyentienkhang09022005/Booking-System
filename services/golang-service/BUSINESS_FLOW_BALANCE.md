# Business Flow: Virtual Balance & Withdrawal System

## Overview

Hệ thống quản lý số dư ảo cho doanh nghiệp trên nền tảng booking. Khi khách thanh toán (ZaloPay/Web3), tiền được cộng vào `company_balance` của doanh nghiệp. Doanh nghiệp có thể rút tiền sau khi admin phê duyệt.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           CUSTOMER PAYS                                 │
│                    (ZaloPay App / MetaMask Web3)                       │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         GOLANG SERVICE (Port 8083)                      │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                    ZaloPay: POST /api/v1/payments/callback         │ │
│  │                                                                    │ │
│  │  1. Verify MAC signature                                           │ │
│  │  2. Find payment by app_trans_id + load relations                  │ │
│  │  3. Update payment status = "Success"                               │ │
│  │  4. Create revenue report (Accommodation/Tour/Transport)           │ │
│  │  5. triggerAddLoyaltyPoints() ──→ POST /loyalty/add-points          │ │
│  │  6. notifyJavaServiceBalanceUpdate() ──→ POST /internal/payment-    │ │
│  │                                                        success      │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                 Web3: POST /web3/payment/execute                   │ │
│  │                                                                    │ │
│  │  1. Verify EIP-712 signature                                       │ │
│  │  2. Execute permit (gasless ERC20 approval)                        │ │
│  │  3. Submit transaction via relayer (Sepolia)                       │ │
│  │  4. UpdateVndPaymentSuccess() - status = "Success"                │ │
│  │  5. Create revenue report (Accommodation/Tour/Transport)           │ │
│  │  6. triggerAddLoyaltyPoints() ──→ POST /loyalty/add-points         │ │
│  │  7. notifyJavaServiceBalanceUpdate() ──→ POST /internal/payment-    │ │
│  │                                                        success      │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         JAVA SERVICE (Port 8080)                         │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │              POST /internal/payment-success                        │ │
│  │  PaymentInternalController.handlePaymentSuccess()                    │ │
│  │                                                                    │ │
│  │  1. Parse JSON (snake_case → camelCase via @JsonProperty)           │ │
│  │  2. CompanyBalanceService.addEarnings()                            │ │
│  │     - Find or create company_balance                                │ │
│  │     - total_earned += amount                                       │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Database Tables

### company_balance
| Column | Type | Description |
|--------|------|-------------|
| id_user | UUID (PK) | Doanh nghiệp |
| total_earned | DECIMAL(14,2) | Tổng tiền đã nhận |
| total_withdrawn | DECIMAL(14,2) | Tổng tiền đã rút |
| locked_amount | DECIMAL(14,2) | Số tiền bị khóa |

**Computed:** `available_balance = total_earned - total_withdrawn - locked_amount`

### company_bank_account
| Column | Type | Description |
|--------|------|-------------|
| id_user | UUID (PK) | Doanh nghiệp |
| bank_name | VARCHAR(100) | Tên ngân hàng |
| account_number | VARCHAR(50) | Số tài khoản |
| account_holder_name | VARCHAR(100) | Tên chủ tài khoản |
| branch | VARCHAR(100) | Chi nhánh |

### withdrawal_request
| Column | Type | Description |
|--------|------|-------------|
| id_request | UUID (PK) | Mã yêu cầu |
| id_user | UUID (FK) | Doanh nghiệp |
| amount | DECIMAL(14,2) | Số tiền muốn rút |
| status | VARCHAR(20) | PENDING / COMPLETED / REJECTED |
| bank_snapshot | TEXT (JSON) | Snapshot thông tin ngân hàng |
| company_note | TEXT | Ghi chú từ doanh nghiệp |
| admin_note | TEXT | Ghi chú từ admin |
| transfer_reference | VARCHAR(100) | Mã giao dịch chuyển khoản |
| created_at | TIMESTAMP | Thời gian tạo |
| processed_at | TIMESTAMP | Thời gian xử lý |
| completed_at | TIMESTAMP | Thời gian hoàn thành |

---

## Business Owner Determination

Khi notification được gửi sang Java, cần xác định doanh nghiệp nào nhận tiền:

| Booking Type | Relationship Chain | Business Owner |
|--------------|---------------------|----------------|
| Accommodation | `payment.id_booking` → `booking.id_accommodation` → `accommodation.id_user` | Chủ chỗ nghỉ |
| Tour | `payment.id_booking_tour` → `booking_tour.id_tour` → `tour.id_user` | Chủ tour |
| Transport | `payment.id_booking_transport` → `booking_transport.id_ticket` → `transport_ticket.id_user` | Chủ vé di chuyển |

**Important:** `FindByAppTransID` phải load đầy đủ relations (Booking, BookingTour, BookingTransport) để `determineBusinessOwner` hoạt động đúng.

---

## Payment Success JSON Structure

```json
{
  "id_payment": "uuid-của-payment",
  "id_user": "business-owner-uuid",
  "amount": 1660000.00,
  "payment_method": "ZaloPay",
  "id_booking": "uuid-hoặc-null",
  "id_booking_tour": "uuid-hoặc-null",
  "id_booking_transport": "uuid-hoặc-null"
}
```

**Note:** Golang gửi snake_case, Java dùng camelCase với `@JsonProperty` annotation để map đúng.

---

## API Endpoints

### Golang Service

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/payments/callback` | ZaloPay callback |
| POST | `/web3/payment/execute` | Web3 payment execution |
| POST | `/web3/payment/prepare` | Prepare EIP-712 data |

### Java Service - Company API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/company/balance` | Lấy số dư công ty |
| GET | `/company/bank-account` | Lấy thông tin ngân hàng |
| PUT | `/company/bank-account` | Cập nhật thông tin ngân hàng |
| POST | `/company/withdrawal/request` | Tạo yêu cầu rút tiền |
| GET | `/company/withdrawal/requests` | Lịch sử rút tiền của doanh nghiệp |

### Java Service - Admin API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin/withdrawal/requests` | Danh sách yêu cầu rút tiền |
| PUT | `/admin/withdrawal/requests/{id}/approve` | Phê duyệt |
| PUT | `/admin/withdrawal/requests/{id}/reject` | Từ chối |

### Java Service - Internal

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/internal/payment-success` | Nhận notification từ Golang |

---

## Withdrawal Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     WITHDRAWAL FLOW                                      │
└─────────────────────────────────────────────────────────────────────────┘

1. BUSINESS: Tạo yêu cầu rút tiền
   POST /company/withdrawal/request
   Body: { "amount": 500000, "company_note": "Rút tiền tháng 5" }

   ├── Validate: bank_account tồn tại?
   ├── Validate: available_balance >= amount?
   ├── Tạo bank_snapshot (JSON serialize company_bank_account)
   └── INSERT withdrawal_request (status = PENDING)

2. ADMIN: Xem danh sách
   GET /admin/withdrawal/requests?status=PENDING

3. ADMIN: Phê duyệt
   PUT /admin/withdrawal/requests/{id}/approve
   Body: { "transfer_reference": "BANK-TXN-12345" }

   ├── UPDATE withdrawal_request SET status = 'COMPLETED'
   ├── total_withdrawn += amount
   └── Gửi notification: "Yêu cầu rút tiền đã được duyệt"

4. ADMIN: Từ chối
   PUT /admin/withdrawal/requests/{id}/reject
   Body: { "admin_note": "Tài khoản không hợp lệ" }

   ├── UPDATE withdrawal_request SET status = 'REJECTED'
   ├── admin_note = ?
   └── Gửi notification: "Yêu cầu rút tiền đã bị từ chối. Lý do: ..."
```

---

## Notification System

### Notification Types
- `system` - Thông báo từ hệ thống (withdrawal approval/rejection)
- `booking` - Thông báo về đặt hàng
- `evaluate` - Thông báo về đánh giá

### RabbitMQ Configuration
- Exchange: `notification.exchange`
- Queue: `notification.queue`
- Routing Key: `notification.routing.key`

### Notification Structure
```json
{
  "idNotification": "uuid",
  "idUser": "recipient-uuid",
  "title": "Tiêu đề thông báo",
  "content": "Nội dung thông báo",
  "type": "system",
  "idRef": "reference-id",
  "isRead": false,
  "createdAt": "2026-05-09T10:00:00"
}
```

### When Notifications Are Sent

| Event | Recipient | Title | Type |
|-------|-----------|-------|------|
| Withdrawal Approved | Business | "Yêu cầu rút tiền đã được duyệt" | system |
| Withdrawal Rejected | Business | "Yêu cầu rút tiền đã bị từ chối" | system |

---

## Sequence Diagrams

### Payment Success (ZaloPay)

```
Customer → ZaloPay App → POST /api/v1/payments/callback
                                          │
                                          ▼
                           PaymentService.ResolveCallBackZaloPay()
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    │                     │                     │
                    ▼                     ▼                     ▼
           UpdateStatus()        CreateRevenue()          goroutine
               │                                                 │
               ▼                                                 ▼
    FindByAppTransID()                              notifyJavaServiceBalanceUpdate()
    (load Booking,                                    │
     BookingTour,                    POST /internal/payment-success
     BookingTransport)                                │
               │                                       ▼
               └─────────────── determineBusinessOwner()
                                          │
                                          ▼
                            CompanyBalanceService.addEarnings()
                                          │
                                          ▼
                               total_earned += amount
```

### Web3 Payment Flow

```
Frontend → POST /web3/payment/execute
                              │
                              ▼
             VndPaymentController.ExecuteVndPayment()
                              │
           ┌──────────────────┼──────────────────┐
           ▼                  ▼                  ▼
    VerifySig()        ExecutePermit()   ExecutePayment()
           │                  │                  │
           └──────────────────┴──────────────────┘
                              │
                              ▼
                    UpdateVndPaymentSuccess()
                              │
                              ▼
                    reportRevenueDetailService
                              │
                              ▼
                    goroutine: triggerAddLoyaltyPoints()
                              │
                              ▼
                    goroutine: notifyJavaServiceBalanceUpdate()
                              │
                              ▼
                    POST /internal/payment-success
```

---

## Error Handling

| Scenario | Handling |
|----------|----------|
| Duplicate payment notification | Idempotency check - ignore if payment_id already processed |
| Company balance not found | Auto-create new balance record for id_user |
| Business owner not found | Log warning, skip balance update |
| Java service unavailable | Goroutine - log error, do not retry |
| Insufficient balance for withdrawal | Return error "Số dư không đủ. Số dư khả dụng: X" |
| No bank account when creating withdrawal | Return error "Vui lòng cập nhật thông tin tài khoản ngân hàng trước" |
| Withdrawal not PENDING when approve/reject | Return error "Chỉ có thể phê duyệt yêu cầu đang chờ" |
| JSON field mismatch (snake_case vs camelCase) | Use @JsonProperty on Java DTOs |

---

## Key Files

### Golang Service
| File | Purpose |
|------|---------|
| `Internal/Services/PaymentService.go` | ZaloPay callback, `notifyJavaServiceBalanceUpdate()` |
| `Internal/Controllers/VndPaymentController.go` | Web3 payment, `triggerAddLoyaltyPoints()`, `notifyJavaServiceBalanceUpdate()` |
| `Internal/Repositories/PaymentRepository.go` | `FindByAppTransID()`, `FindByIDPayment()` với relation loading |
| `Internal/Models/Tour.go` | Tour model (business owner lookup) |
| `Internal/Models/TransportTicket.go` | TransportTicket model (business owner lookup) |

### Java Service
| File | Purpose |
|------|---------|
| `Controllers/PaymentInternalController.java` | Nhận notification từ Golang |
| `Controllers/CompanyController.java` | API cho doanh nghiệp (balance, bank-account, withdrawal) |
| `Controllers/AdminWithdrawalController.java` | API cho admin (approve/reject) |
| `Services/CompanyBalanceService.java` | Xử lý balance, withdrawal, notification |
| `Dto/Request/PaymentSuccessNotification.java` | DTO với @JsonProperty cho snake_case |
| `Models/CompanyBalance.java` | Entity company_balance |
| `Models/CompanyBankAccount.java` | Entity company_bank_account |
| `Models/WithdrawalRequest.java` | Entity withdrawal_request |
| `Configs/RabbitConfig.java` | RabbitMQ notification config |

---

## Verification Checklist

- [ ] ZaloPay payment → balance tăng + loyalty points được cộng
- [ ] Web3 payment → balance tăng + loyalty points được cộng
- [ ] Idempotency: duplicate notification không tăng double balance
- [ ] Tạo withdrawal → check available_balance đủ không
- [ ] Admin approve → balance được cộng, notification được gửi
- [ ] Admin reject → notification được gửi với lý do
- [ ] Không có bank_account → không cho tạo withdrawal request