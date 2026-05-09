# Business Flow: Payment to Balance Update

## Overview

Flow giải thích cách thanh toán (ZaloPay/Web3) được xử lý, cập nhật balance cho doanh nghiệp, và gửi notification sang Java service.

---

## Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           CUSTOMER PAYS                                 │
│                    (ZaloPay App / MetaMask Web3)                       │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         GOLANG SERVICE                                  │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                    ZaloPay: /api/v1/payments/callback               │ │
│  │                                                                    │ │
│  │  1. Verify MAC signature                                           │ │
│  │  2. Find payment by app_trans_id                                   │ │
│  │  3. Update payment status = "Success"                              │ │
│  │  4. Create revenue report (Accommodation/Tour/Transport)           │ │
│  │  5. triggerAddLoyaltyPoints(payment) ──→ POST /loyalty/add-points  │ │
│  │  6. notifyJavaServiceBalanceUpdate(payment) ──→ /internal/payment- │ │
│  │                                                        success     │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                 Web3: /web3/payment/execute                        │ │
│  │                                                                    │ │
│  │  1. Verify EIP-712 signature                                       │ │
│  │  2. Execute permit (gasless approval)                             │ │
│  │  3. Submit transaction via relayer                                 │ │
│  │  4. UpdateVndPaymentSuccess() - payment status = "Success"         │ │
│  │  5. Create revenue report                                          │ │
│  │  6. triggerAddLoyaltyPoints(payment) ──→ POST /loyalty/add-points  │ │
│  │  7. notifyJavaServiceBalanceUpdate(payment) ──→ /internal/payment- │ │
│  │                                                        success     │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────┬────────────────────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │   JAVA SERVICE  │
                    │                 │
                    │ /internal/      │
                    │ payment-success  │
                    │                 │
                    │ 1. Idempotency  │
                    │    check         │
                    │ 2. Find/create   │
                    │    CompanyBalance│
                    │ 3. total_earned +=│
                    │    amount        │
                    └─────────────────┘
```

---

## Business Owner Determination

Khi notification được gửi sang Java, cần xác định doanh nghiệp nào nhận tiền:

| Payment Type | Booking Reference | Business Owner Source |
|--------------|-------------------|----------------------|
| Accommodation | payment.id_booking | booking → accommodation → users.id_user |
| Tour | payment.id_booking_tour | booking_tour → tour → users.id_user |
| Transport | payment.id_booking_transport | booking_transport → transport_ticket → users.id_user |

---

## Payment Success Data Structure

Khi Golang gọi sang Java `/internal/payment-success`:

```json
{
  "id_payment": "uuid",
  "id_user": "business_owner_uuid",
  "amount": 1000000.00,
  "payment_method": "ZaloPay",
  "id_booking": "uuid_or_null",
  "id_booking_tour": "uuid_or_null",
  "id_booking_transport": "uuid_or_null"
}
```

---

## Balance Update Logic (Java)

```
1. Check: payment_id đã xử lý chưa? (idempotency)
   - Nếu đã xử lý → return (ignore duplicate)

2. Find or Create CompanyBalance:
   - SELECT * FROM company_balance WHERE id_user = ?
   - Nếu không tồn tại → INSERT mới

3. Update balance:
   UPDATE company_balance
   SET total_earned = total_earned + :amount
   WHERE id_user = :id_user

4. Record processed payment:
   INSERT INTO processed_payment_notifications (id_payment, processed_at)
   ON CONFLICT DO NOTHING
```

---

## Withdrawal Flow

```
1. Business tạo withdrawal request:
   POST /company/withdrawal/request
   Body: { "amount": 500000, "company_note": "Rút tiền tháng 5" }

   Logic:
   - Lấy bank_snapshot từ company_bank_account (serialize to JSON)
   - Check available_balance >= amount
     available = total_earned - total_withdrawn - locked_amount
   - INSERT withdrawal_request

2. Admin xem danh sách:
   GET /admin/withdrawal/requests?status=PENDING

3. Admin phê duyệt:
   PUT /admin/withdrawal/requests/{id}/approve
   Body: { "transfer_reference": "BANK-TXN-12345" }

   Logic:
   - UPDATE withdrawal_request SET status = 'COMPLETED',
     processed_at = NOW(), transfer_reference = ?
   - UPDATE company_balance SET total_withdrawn = total_withdrawn + amount

4. Admin từ chối:
   PUT /admin/withdrawal/requests/{id}/reject
   Body: { "admin_note": "Tài khoản không hợp lệ" }

   Logic:
   - UPDATE withdrawal_request SET status = 'REJECTED',
     processed_at = NOW(), admin_note = ?
```

---

## Status Values

### Withdrawal Request Status
- `PENDING` - Chờ admin duyệt
- `COMPLETED` - Đã duyệt, đã chuyển tiền
- `REJECTED` - Bị từ chối

---

## Key Files

### Golang Service
| File | Change |
|------|--------|
| `PaymentService.go` | Thêm `notifyJavaServiceBalanceUpdate()` trong `ResolveCallBackZaloPay()` |
| `VndPaymentController.go` | Thêm `triggerAddLoyaltyPoints()` và `notifyJavaServiceBalanceUpdate()` trong `ExecuteVndPayment()` |

### Java Service
| File | Purpose |
|------|---------|
| `PaymentInternalController.java` | Nhận notification từ Golang |
| `CompanyBalanceService.java` | Xử lý balance, withdrawal |
| `CompanyController.java` | API cho doanh nghiệp |
| `AdminWithdrawalController.java` | API cho admin |

---

## Sequence: Payment Success (ZaloPay)

```
1. ZaloPay server POST /api/v1/payments/callback
2. PaymentService.ResolveCallBackZaloPay():
   a. Verify MAC signature
   b. Find payment by app_trans_id
   c. paymentRepository.UpdateStatus("Success")
   d. reportRevenueDetailService.CreateReportRevenueDetail()
   e. goroutine: triggerAddLoyaltyPoints(payment) → POST /loyalty/add-points
   f. goroutine: notifyJavaServiceBalanceUpdate(payment, "ZaloPay")
3. notifyJavaServiceBalanceUpdate():
   a. determineBusinessOwner(payment) → id_user
   b. POST /internal/payment-success {id_payment, id_user, amount, ...}
4. Java service receives → addEarnings()
5. company_balance.total_earned += amount
```

---

## Sequence: Payment Success (Web3)

```
1. Frontend POST /web3/payment/execute {signature, order}
2. VndPaymentController.ExecuteVndPayment():
   a. Verify EIP-712 signature
   b. Execute permit if needed
   c. blockchainService.ExecutePayment()
   d. paymentRepository.UpdateVndPaymentSuccess(txHash)
   e. reportRevenueDetailService.CreateReportRevenueDetail()
   f. goroutine: triggerAddLoyaltyPoints(payment) → POST /loyalty/add-points
   g. goroutine: notifyJavaServiceBalanceUpdate(payment, "VNDStablecoin")
3. notifyJavaServiceBalanceUpdate():
   a. determineBusinessOwner(payment) → id_user
   b. POST /internal/payment-success {id_payment, id_user, amount, ...}
4. Java service receives → addEarnings()
5. company_balance.total_earned += amount
```

---

## Error Handling

| Scenario | Handling |
|----------|----------|
| Duplicate payment notification | Idempotency check - ignore if payment_id already processed |
| Company balance not found | Auto-create new balance record for id_user |
| Business owner not found | Log warning, skip balance update |
| Java service unavailable | Goroutine - log error, do not retry |
| Insufficient balance for withdrawal | Return error "Số dư không đủ" |