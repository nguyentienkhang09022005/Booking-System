# 05 — Phân tầng Voucher (Voucher Tiering)

> **Mở rộng so với Đồ án 1.** Voucher không còn là mã giảm giá đơn giản mà được **phân tầng theo nhà tài trợ và phạm vi áp dụng**, hỗ trợ cả sản phẩm mới (tour, vé).

## 1. Hai tầng voucher
| Tầng | sponsorType | accommodation | Phạm vi |
|------|-------------|---------------|---------|
| **System Voucher** | `SYSTEM_TYPE` | `null` | Toàn hệ thống (theo `applyTo`), do Admin tạo & duyệt |
| **Business Voucher** | `BUSINESS_TYPE` | id cụ thể | Chỉ cho accommodation của doanh nghiệp đó, cần Admin duyệt |

## 2. Phạm vi áp dụng (`applyTo`)
`APPLY_TO_ROOM` · `APPLY_TO_SERVICE` · `APPLY_TO_TOUR` · `APPLY_TO_TICKET` · `APPLY_TO_ALL` — quyết định voucher áp cho phòng / dịch vụ / tour / vé / tất cả.

## 3. Entity `Voucher` (bảng `voucher`)
idVoucher, accommodation (FK, null cho system), nameVoucher, **code** (unique), discountPercent, **minDiscountAmount / maxDiscountAmount**, sponsorType, applyTo, startDate/endDate, quantity, conditions, approved, createAt.

## 4. Công thức giảm giá (có cận min/max)
```
discount = min( max(applyAmount * discountPercent/100, minDiscountAmount), maxDiscountAmount )
```
Ví dụ: applyAmount=1.000.000, 20%, min=50.000, max=200.000 → discount = 200.000.

## 5. API — base path `/voucher`
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/list-voucher-approved-by-accommodation?idAccommodation=&idUser=` | Voucher đã duyệt của 1 accommodation |
| GET | `/list-voucher-approved?idUser=` | Voucher hệ thống đã duyệt |
| GET | `/list-voucher-not-approved` | Voucher chờ duyệt (Admin) |
| GET | `/list-voucher?idAccommodation=` | Tất cả voucher của accommodation |
| POST | `/business/creation` · PATCH `/business/update?idVoucher=` | CRUD voucher doanh nghiệp |
| POST | `/system/creation` · PATCH `/system/update?idVoucher=` | CRUD voucher hệ thống |
| DELETE | `/delele?idVoucher=` | Xóa voucher |

> Hai luồng tạo/cập nhật tách biệt (business vs system); có ErrorCode chặn sửa nhầm loại (`SYSTEM_VOUCHER_MODIFICATION_DENIED`, `BUSINESS_VOUCHER_MODIFICATION_DENIED`).

## 6. Liên kết khi đặt chỗ
Voucher được áp vào booking (phòng/tour/vé) qua `AppliedVoucher`; người dùng có thể lưu voucher qua `UserVoucher` (`/user-voucher`). Repository có `decreaseVoucherQuantity` giảm tồn khi dùng.

## 7. Vị trí mã nguồn
`Models/Voucher.java`, `Repositories/Jpa/VoucherRepository.java`, `Services/VoucherService.java`, `Controllers/VoucherController.java`, `Constant/PredefinedSponsorType.java`, `PredefinedApplyTo.java`. Spec gốc: `Docs/SPEC_VOUCHER.md`.
