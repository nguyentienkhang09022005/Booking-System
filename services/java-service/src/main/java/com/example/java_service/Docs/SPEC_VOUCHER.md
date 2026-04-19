# Voucher Module Specification (After Refactor)

## 1. Overview

Module Voucher quản lý các mã giảm giá trong hệ thống BookingNext. Voucher có 2 loại:
- **System Voucher**: Được tạo bởi hệ thống, áp dụng cho tất cả
- **Business Voucher**: Được tạo bởi doanh nghiệp (accommodation), chỉ áp dụng cho accommodation đó

## 2. Entity

### Voucher.java

```java
@Entity
@Table(name = "voucher")
public class Voucher {
    UUID idVoucher;                    // Primary Key
    Accommodation accommodation;       // FK -> Accommodation (null cho system voucher)
    String nameVoucher;               // Tên voucher
    String code;                      // Mã voucher (unique)
    BigDecimal discountPercent;        // Phần trăm giảm giá
    BigDecimal minDiscountAmount;      // Số tiền giảm tối thiểu
    BigDecimal maxDiscountAmount;     // Số tiền giảm tối đa
    String sponsorType;                // SYSTEM_TYPE / BUSINESS_TYPE
    String applyTo;                    // Áp dụng cho: ROOM, SERVICE, TOUR, TICKET, ALL
    LocalDate startDate;              // Ngày bắt đầu
    LocalDate endDate;                // Ngày kết thúc
    Integer quantity;                  // Số lượng voucher
    String conditions;                // Điều kiện sử dụng
    Boolean approved;                  // Đã phê duyệt
    LocalDateTime createAt;            // Ngày tạo
}
```

## 3. Constants

### PredefinedSponsorType.java
```java
SYSTEM_TYPE    // Voucher hệ thống
BUSINESS_TYPE  // Voucher doanh nghiệp
```

### PredefinedApplyTo.java
```java
APPLY_TO_ROOM      // Áp dụng cho phòng
APPLY_TO_SERVICE   // Áp dụng cho dịch vụ
APPLY_TO_TOUR      // Áp dụng cho tour
APPLY_TO_TICKET    // Áp dụng cho vé
APPLY_TO_ALL       // Áp dụng cho tất cả
```

## 4. API Endpoints

Base Path: `/voucher`

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/list-voucher-approved-by-accommodation?idAccommodation={uuid}&idUser={uuid}` | Lấy voucher của accommodation đã duyệt |
| GET | `/list-voucher-approved?idUser={uuid}` | Lấy voucher hệ thống đã duyệt |
| GET | `/list-voucher-not-approved` | Lấy voucher chưa phê duyệt |
| GET | `/list-voucher?idAccommodation={uuid}` | Lấy voucher của accommodation |
| POST | `/business/creation` | Tạo voucher doanh nghiệp |
| PATCH | `/business/update?idVoucher={uuid}` | Cập nhật voucher doanh nghiệp |
| POST | `/system/creation` | Tạo voucher hệ thống |
| PATCH | `/system/update?idVoucher={uuid}` | Cập nhật voucher hệ thống |
| DELETE | `/delele?idVoucher={uuid}` | Xóa voucher |

## 5. Service Layer

### VoucherService.java

| Method | Mô tả |
|--------|--------|
| `listVouchersApprovedByAccommodation(UUID idAccommodation, UUID idUser)` | Lấy voucher của accommodation |
| `listVouchersApproved(UUID idUser)` | Lấy voucher hệ thống đã duyệt |
| `listVouchersNotApproved()` | Lấy voucher chưa phê duyệt |
| `listVouchersOfAccommodation(UUID idAccommodation)` | Lấy tất cả voucher của accommodation |
| `saveBusinessVoucher(BusinessVoucherCreationRequest request)` | Tạo voucher doanh nghiệp |
| `saveSystemVoucher(SystemVoucherCreationRequest request)` | Tạo voucher hệ thống |
| `updateBusinessVoucher(VoucherUpdateRequest request, UUID idVoucher)` | Cập nhật voucher doanh nghiệp |
| `updateSystemVoucher(VoucherUpdateRequest request, UUID idVoucher)` | Cập nhật voucher hệ thống |
| `deleteVoucher(UUID idVoucher)` | Xóa voucher |

## 6. Repository

### VoucherRepository.java

```java
existsVoucherByCode(String code);
findApprovedVouchersForAccommodationAndSystem(UUID idAccommodation);
findVoucherByApprovedFalse();
findVoucherByAccommodation_IdAccommodation(UUID idAccommodation);
decreaseVoucherQuantity(UUID idVoucher);
findAllWithAccommodationAndUsersByIds(List<UUID> ids);
```

## 7. DTOs

### Request DTOs
- `BusinessVoucherCreationRequest` - Tạo voucher doanh nghiệp
- `SystemVoucherCreationRequest` - Tạo voucher hệ thống
- `VoucherUpdateRequest` - Cập nhật voucher

### Response DTOs
- `VoucherViewResponse` - Chi tiết voucher cho user xem
- `VoucherViewAllResponse` - Chi tiết voucher đầy đủ (admin)
- `VoucherEditResponse` - Chi tiết voucher để chỉnh sửa

## 8. Voucher Calculation Logic

```
Discount Amount = min(
    max(applyAmount * discountPercent / 100, minDiscountAmount),
    maxDiscountAmount
)
```

Ví dụ:
- applyAmount = 1,000,000 VND
- discountPercent = 20%
- minDiscountAmount = 50,000 VND
- maxDiscountAmount = 200,000 VND

```
baseDiscount = 1,000,000 * 20 / 100 = 200,000
finalDiscount = min(max(200,000, 50,000), 200,000) = 200,000
```

## 9. Related Entities

| Entity | Relationship |
|--------|--------------|
| `Accommodation` | @ManyToOne (nullable cho system voucher) |
| `AppliedVoucher` | @OneToMany (mappedBy="voucher") |
| `UserVoucher` | @OneToMany (mappedBy="voucher") |

## 10. File Locations

| Type | Path |
|------|------|
| Entity | `Models/Voucher.java` |
| Repository | `Repositories/Jpa/VoucherRepository.java` |
| Service | `Services/VoucherService.java` |
| Controller | `Controllers/VoucherController.java` |
| Constants | `Constant/PredefinedSponsorType.java` |
| | `Constant/PredefinedApplyTo.java` |

## 11. Voucher Types

### System Voucher
- Được tạo bởi admin hệ thống
- `accommodation = null`
- Áp dụng cho tất cả accommodation (theo applyTo)
- Cần được duyệt trước khi hoạt động

### Business Voucher
- Được tạo bởi doanh nghiệp cho accommodation của họ
- `accommodation = id_accommodation cụ thể`
- Chỉ áp dụng cho accommodation đó
- Cần được duyệt trước khi hoạt động
