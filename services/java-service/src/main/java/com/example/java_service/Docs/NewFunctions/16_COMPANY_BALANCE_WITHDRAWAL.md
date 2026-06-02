# 16 — Ví số dư doanh nghiệp & Rút tiền (Company Balance & Withdrawal)

> **Mới ở Đồ án 2.** Đồ án 1 chỉ tổng hợp **báo cáo doanh thu**; chưa có "ví" thực sự. Đồ án 2 bổ sung **ví số dư ảo** cho doanh nghiệp: mỗi lần khách thanh toán thành công (qua **ZaloPay hoặc Web3**) tiền được cộng vào ví; doanh nghiệp tạo **yêu cầu rút tiền** về tài khoản ngân hàng, Admin duyệt/từ chối.

> Áp dụng cho **mọi kênh thanh toán** (không chỉ blockchain) — xem cơ chế thanh toán ở `11_BLOCKCHAIN_PAYMENT.md`.

## 1. Mô hình dữ liệu
**`company_balance`** (mỗi doanh nghiệp 1 dòng)
| Cột | Ý nghĩa |
|-----|---------|
| id_user (PK) | Doanh nghiệp |
| total_earned | Tổng tiền đã nhận |
| total_withdrawn | Tổng đã rút |
| locked_amount | Số tiền đang khóa (yêu cầu rút đang chờ) |

→ `available_balance = total_earned − total_withdrawn − locked_amount`.

**`company_bank_account`**: bank_name, account_number, account_holder_name, branch.
**`withdrawal_request`**: amount, **status** (`PENDING/COMPLETED/REJECTED`), bank_snapshot (JSON), company_note, admin_note, transfer_reference, created_at/processed_at/completed_at.

## 2. Luồng nghiệp vụ
```
Khách thanh toán thành công (golang-service: ZaloPay callback / Web3 execute)
   → POST /internal/payment-success (xác định chủ doanh nghiệp theo booking)
   → CompanyBalanceService.addEarnings(): total_earned += amount

Doanh nghiệp: thiết lập tài khoản ngân hàng → tạo yêu cầu rút (khóa locked_amount)
   → Admin duyệt: ghi transfer_reference, total_withdrawn += amount, status=COMPLETED
   → hoặc Admin từ chối: ghi admin_note, mở khóa, status=REJECTED
```
Xác định chủ doanh nghiệp nhận tiền theo chuỗi quan hệ: booking phòng→accommodation→id_user; booking tour→tour→id_user; booking transport→ticket→id_user.

## 3. API
**Doanh nghiệp** — base path `/company`
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/balance?idUser=` | Xem số dư |
| GET | `/bank-account?idUser=` | Xem tài khoản ngân hàng |
| PUT | `/bank-account?idUser=` | Cập nhật tài khoản ngân hàng |
| POST | `/withdrawal/request?idUser=` | Tạo yêu cầu rút tiền |
| GET | `/withdrawal/requests?idUser=` | Lịch sử yêu cầu rút của mình |

**Admin** — base path `/admin/withdrawal`
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/requests?status=&page=&size=` | Danh sách yêu cầu (lọc theo trạng thái) |
| PUT | `/requests/{id}/approve` | Duyệt (body: `transferReference`) |
| PUT | `/requests/{id}/reject` | Từ chối (body: `adminNote`) |

**Nội bộ (golang → java)** — `POST /internal/payment-success` (body `PaymentSuccessNotification`: idPayment, idUser, amount, paymentMethod, idBooking/idBookingTour/idBookingTransport).

## 4. Service
`CompanyBalanceService`: `addEarnings`, `getBalance`, `getBankAccount`, `updateBankAccount`, `createWithdrawalRequest`, `getUserWithdrawalRequests`, `getWithdrawalRequests`, `approveWithdrawal`, `rejectWithdrawal`.

## 5. Vị trí mã nguồn
`Models/CompanyBalance.java`, `CompanyBankAccount.java`, `WithdrawalRequest.java`; `Services/CompanyBalanceService.java`; `Controllers/CompanyController.java`, `AdminWithdrawalController.java`, `PaymentInternalController.java`; `Dto/Request/PaymentSuccessNotification.java`, `WithdrawalRequestCreation.java`, `WithdrawalApprovalRequest.java`, `WithdrawalRejectionRequest.java`, `CompanyBankAccountRequest.java`. Tài liệu kỹ thuật: `golang-service/BUSINESS_FLOW_BALANCE.md`.
