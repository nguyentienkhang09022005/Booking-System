# 01 — Tour du lịch

> **Mới ở Đồ án 2.** Đồ án 1 chỉ có sản phẩm khách sạn/phòng/dịch vụ. Đồ án 2 bổ sung **Tour** — gói dịch vụ du lịch trọn gói (lịch trình, điểm đón/trả, dịch vụ kèm theo) do doanh nghiệp đăng bán.

## 1. Mục đích
Cho phép doanh nghiệp (Company) đăng và quản lý tour; khách hàng xem chi tiết và đặt tour. Admin phê duyệt trước khi tour hiển thị công khai.

## 2. Entity `Tour` (bảng `tour`)
| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| idTour | UUID | Khóa chính |
| users | Users (FK) | Doanh nghiệp cung cấp tour |
| tourName / description | String | Tên / mô tả |
| durationMinutes | Integer | Thời lượng (phút) |
| meetingPoint / dropOffPoint | String | Điểm đón / điểm trả |
| cancellationPolicy | String | Chính sách hủy |
| languages | String | Ngôn ngữ hỗ trợ |
| inclusions / exclusions / additionalInfo | List\<String> (jsonb) | Bao gồm / không bao gồm / thông tin thêm |
| adultPrice / childPrice | BigDecimal | Giá người lớn / trẻ em |
| status | String | `Available` / `Sold Out` |
| approved | Boolean | Đã phê duyệt |
| tourSchedules | List\<TourSchedule> | Lịch trình theo ngày (@OneToMany) |

## 3. API — base path `/tour`
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/detail?idTour=` | Chi tiết tour (kèm schedules, ảnh, owner) |
| GET | `/get-all` | Tất cả tour |
| GET | `/get-list-approved` | Tour đã duyệt (công khai) |
| GET | `/get-list-not-approved` | Tour chờ duyệt (Admin) |
| GET | `/list-of-user?idUser=` | Tour của một doanh nghiệp |
| POST | `/creation` | Tạo tour |
| PATCH | `/update?idTour=` | Cập nhật tour |
| DELETE | `/delete?idTour=` | Xóa tour |
| PATCH | `/approve?idTour=` | Admin phê duyệt |

## 4. Service / Repository
- `TourService`: `getTourDetail`, `getListTourApproved/NotApproved`, `getListTourCreatedByUser`, `saveTour`, `updateTour`, `deleteTour`, `approveTour`.
- `TourRepository`: `findByApprovedTrue/False`, `findByUsers_IdUser`, `findAllToursWithSchedules`, `searchByName`.

## 5. Luồng trạng thái
```
Tạo tour (approved=false) → Admin duyệt → approved=true (hiển thị)
status: Available → Sold Out (khi hết chỗ)
```

## 6. Quan hệ
`Tour` 1—N `TourSchedule`, 1—N `BookingTour`, N—1 `Users` (chủ tour). Tour cũng là một đối tượng được hỗ trợ bởi Filter/Search mới và được Chatbot/AI Scheduler tra cứu.

## 7. Vị trí mã nguồn
`Models/Tour.java`, `Repositories/Jpa/TourRepository.java`, `Services/TourService.java`, `Controllers/TourController.java`, `Constant/PredefinedStatusTour.java`. Spec gốc: `Docs/SPEC_TOUR.md`.
