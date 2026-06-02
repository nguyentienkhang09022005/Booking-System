# 06 — Bản tin (Bulletin Board)

> **Mới ở Đồ án 2.** Kênh thông báo / tin khuyến mãi hiển thị tới người dùng, với cơ chế duyệt phân quyền giữa Admin và Business.

## 1. Nghiệp vụ
- **Admin tạo**: duyệt **tự động** (`APPROVED`), chọn `targetAudience` (ALL/CUSTOMER/BUSINESS), có quyền đánh dấu `isFeatured`.
- **Business tạo**: mặc định `targetAudience = CUSTOMER`, trạng thái `PENDING`, `isFeatured = false`, chờ Admin duyệt mới hiển thị.
- Trạng thái: `PENDING / APPROVED / REJECTED / EXPIRED`. Chỉ **Admin** được set `isFeatured`.

## 2. Entity `BulletinBoard` (bảng `bulletin_board`)
idBulletin, idUser (tác giả), title, content (HTML/Markdown), targetAudience, status, **isFeatured**, isApproved, startDate/endDate, createdAt, approvedAt. Ảnh lưu ở **MongoDB** (`images`, refId→bulletin, type="bulletin").

## 3. 5 nhóm tin cho Customer (`GET /bulletin/list?audience=CUSTOMER`)
| Key | Logic |
|-----|-------|
| `leaderboard` | `isFeatured=true`, mới nhất, LIMIT 3 |
| `featuredGrid` | `isFeatured=true` còn lại (trừ leaderboard) |
| `newestScroll` | mới nhất (trong 3 ngày), trừ featured |
| `expiringSoon` | `endDate` trong 5 ngày tới, sắp hết hạn lên đầu |
| `regularGrid` | còn lại, trừ tất cả nhóm trên |

## 4. API — base path `/bulletin`
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/creation` | Tạo bản tin |
| GET | `/list` | 5 nhóm tin cho Customer |
| GET | `/{id}` | Chi tiết |
| PATCH | `/{id}` | Cập nhật (Admin chỉnh `isFeatured`) |
| DELETE | `/{id}?idUser=` | Xóa |
| GET | `/my-bulletins` | Tin đã tạo (lọc theo status) |
| GET | `/pending` · `/approved` · `/rejected` | Danh sách cho Admin |
| PATCH | `/{id}/approve` · `/{id}/reject` | Admin duyệt / từ chối |

## 5. Tích hợp sự kiện
Khi Admin approve/reject bản tin của Business → gửi **notification type=SYSTEM** tới chủ doanh nghiệp (kèm lý do nếu từ chối).

## 6. Phân quyền (tóm tắt)
Business: tạo (PENDING, isFeatured=false), sửa tin **chưa duyệt** của mình, xóa. Admin: tạo (APPROVED), duyệt/từ chối, chỉnh `isFeatured`, thao tác mọi tin.

## 7. Vị trí mã nguồn
`Models/BulletinBoard.java`, `Repositories/Jpa/BulletinBoardRepository.java`, `Services/BulletinBoardService.java`, `Controllers/BulletinBoardController.java`, `Constant/PredefinedStatusBulletin.java`. Spec gốc: `Docs/FEATURE_01_BULLETIN_BOARD.md`, SQL: `Docs/SCRIPT_01_BULLETIN_BOARD.sql`.
