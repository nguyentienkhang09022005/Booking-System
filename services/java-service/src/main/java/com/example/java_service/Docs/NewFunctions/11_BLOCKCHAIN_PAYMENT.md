# 11 — Thanh toán bằng Blockchain (Web3 VND Stablecoin)

> **Mới ở Đồ án 2.** Bên cạnh ZaloPay (Đồ án 1), bổ sung kênh thanh toán **on-chain** bằng đồng **VND stablecoin (ERC-20)** trên **Ethereum Sepolia testnet**, dùng **EIP-712 gasless** (relayer trả phí gas, người dùng chỉ ký ví MetaMask). Kèm theo là **ví số dư doanh nghiệp** và quy trình **rút tiền**.

## 1. Hai kênh thanh toán
| Kênh | Loại | Phí gas | Trải nghiệm |
|------|------|---------|-------------|
| ZaloPay | Truyền thống | App | Quét QR / app |
| **VND Stablecoin** | Blockchain (EIP-712) | **Relayer trả (gasless)** | Ký bằng ví MetaMask |

Booking endpoints (`/booking-tour/creation`, `/booking-transport/creation`, booking phòng) nhận `paymentMethod` & `payerAddress` để chọn kênh.

## 2. Smart contracts (`contracts/`, Solidity + Foundry)
- **`VNDToken`** — token ERC-20 stablecoin (đại diện VND), có `permit` (EIP-2612).
- **`PaymentGateway`** — nhận thanh toán, dùng nonce + chữ ký EIP-712 để xác thực.
- Triển khai trên Sepolia (địa chỉ cấu hình qua biến môi trường — không nêu giá trị ở đây).

## 3. Luồng thanh toán Web3 (gasless)
```
1. FE gọi golang-service "prepare" → lấy nonce, dựng EIP-712 TypedData.
2. Người dùng ký bằng MetaMask (không tốn gas).
3. FE gửi "execute" kèm chữ ký → golang-service:
   - Verify chữ ký EIP-712 (recover signer, so địa chỉ).
   - Thực thi permit (approve ERC-20 gasless) + submit giao dịch qua relayer trên Sepolia.
   - UpdateVndPaymentSuccess (status=Success), tạo revenue report.
   - triggerAddLoyaltyPoints → POST /loyalty/add-points.
   - notifyJavaServiceBalanceUpdate → POST /internal/payment-success.
```
Endpoint Web3 (golang-service): `POST /web3/payment/prepare`, `POST /web3/payment/execute`.

## 4. Cầu nối golang → java (`/internal/payment-success`)
`PaymentInternalController.handlePaymentSuccess()` nhận JSON (snake_case) gồm idPayment, idUser (chủ doanh nghiệp), amount, paymentMethod, idBooking/idBookingTour/idBookingTransport → gọi `CompanyBalanceService.addEarnings()` cộng `total_earned`.
Xác định chủ doanh nghiệp theo chuỗi quan hệ booking → accommodation/tour/ticket → `id_user`.

## 5. Ví số dư & rút tiền (java-service)
Sau khi thanh toán thành công (ZaloPay hoặc Web3), tiền được cộng vào ví số dư của doanh nghiệp và có quy trình rút tiền do Admin duyệt. Chi tiết: xem **`16_COMPANY_BALANCE_WITHDRAWAL.md`**.

## 6. Vị trí mã nguồn
- java-service: `Controllers/PaymentInternalController.java`, `CompanyController.java`, `AdminWithdrawalController.java`; `Services/CompanyBalanceService.java`.
- golang-service: `Internal/...` (Web3 + ZaloPay), `Routes/`, contracts ở `contracts/src/PaymentGateway.sol`, `VNDToken.sol`.
- Tài liệu kỹ thuật đầy đủ: `Docs/WEB3_PAYMENT_ECOSYSTEM.md`, `golang-service/BUSINESS_FLOW_BALANCE.md`.

> Lưu ý bảo mật: chữ ký EIP-712 được verify phía server trước khi submit; relayer chịu phí gas; mọi giá trị bí mật (RPC, khóa, địa chỉ contract) nằm trong biến môi trường (không trình bày tại đây).
