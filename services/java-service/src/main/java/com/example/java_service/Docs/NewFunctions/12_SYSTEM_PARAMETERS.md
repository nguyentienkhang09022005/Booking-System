# 12 — Tham số hệ thống (System Parameter)

> **Mới ở Đồ án 2.** Trung tâm cấu hình runtime: thay đổi các hằng số nghiệp vụ (tỷ lệ điểm thưởng, timeout thanh toán, giới hạn AI...) **mà không cần deploy lại**.

## 1. Entity `SystemParameter` (bảng `system_parameter`)
| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| paramKey | String (PK) | Khóa tham số (bất biến sau khi tạo) |
| paramValue | TEXT | Giá trị |
| description | TEXT | Mô tả |
| updatedAt | LocalDateTime | Lần cập nhật cuối |

## 2. Cơ chế truy cập (Singleton + cache)
`SystemParameterService` nạp toàn bộ tham số vào `ConcurrentHashMap` khi khởi động; `getParamValue(key)` đọc từ cache; khi cập nhật thì ghi DB rồi invalidate/nạp lại cache. Nhờ vậy truy cập nhanh và đồng bộ.

## 3. API — base path `/system-parameter`
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/list-system-parameter` | Lấy tất cả tham số |
| PATCH | `/update?paramKey=` | Cập nhật giá trị tham số |

## 4. Tham số tiêu biểu
| Key | Mặc định | Ý nghĩa |
|-----|----------|---------|
| `VND_PER_LOYALTY_POINT` | 1000 | 1000đ = 1 điểm thưởng |
| `PAYMENT_TIMEOUT_MINUTES` | 15 | Hết hạn giữ chỗ/thanh toán |
| `PAYMENT_RETRY_COUNT` | 3 | Số lần thử lại thanh toán |
| `MAX_BOOKING_ADVANCE_DAYS` / `MIN_BOOKING_ADVANCE_HOURS` | 30 / 2 | Giới hạn đặt trước |
| `AI_MAX_TOUR_SUGGESTIONS`, `AI_MAX_SERVICE_PER_ACCOMMODATION`, `AI_FUZZY_MATCH_THRESHOLD` | 3 / 3 / 0.7 | Tham số cho AI Scheduler & search |

## 5. Cách dùng trong code
```java
String vndPerPoint = systemParameterService.getParamValue("VND_PER_LOYALTY_POINT");
int points = totalPrice / Integer.parseInt(vndPerPoint);
```
Quy tắc: key phải tồn tại, value không rỗng, key không đổi sau khi tạo.

## 6. Vị trí mã nguồn
`Models/SystemParameter.java`, `Repositories/Jpa/SystemParameterRepository.java`, `Services/SystemParameterService.java`, `Controllers/SystemParameterController.java`, `Mappers/SystemParameterMapper.java`. Spec gốc: `Docs/SPEC_PARAMETER_SYSTEM.md`.
