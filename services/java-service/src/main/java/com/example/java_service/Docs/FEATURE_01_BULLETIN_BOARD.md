# Bulletin Board - Bảng Tin Thông Báo

## 1. Mục đích
Hệ thống bảng tin cho phép Admin và Business tạo nội dung thông báo, tin khuyến mãi hiển thị đến người dùng trong hệ thống với cơ chế duyệt phù hợp.

## 2. Mô tả nghiệp vụ

### Luồng hoạt động
- **Admin tạo bản tin**: Duyệt tự động, chọn target audience (CUSTOMER/BUSINESS/ALL), có quyền đánh dấu `isFeatured`
- **Business tạo bản tin**: Mặc định target = CUSTOMER, trạng thái PENDING, `isFeatured = false`, chờ Admin duyệt mới hiển thị

### Target Audience
- `ALL` - Toàn hệ thống (Admin only)
- `CUSTOMER` - Chỉ khách hàng
- `BUSINESS` - Chỉ đối tác doanh nghiệp (Admin only)

### Trạng thái bản tin
- `PENDING` - Chờ duyệt (Business tạo)
- `APPROVED` - Đã duyệt, hiển thị
- `REJECTED` - Bị từ chối
- `EXPIRED` - Hết hạn

### Tin nổi bật (isFeatured)
- Chỉ **Admin** mới có quyền đánh dấu `isFeatured = true`
- Business tạo bản tin luôn có `isFeatured = false` (bất kể truyền giá trị gì)
- Khi Admin duyệt bản tin của Business, Admin có thể toggle `isFeatured` nếu cần

## 3. Database Design

### PostgreSQL Table: `bulletin_board`

| Column | Type | Description |
|--------|------|-------------|
| id_bulletin | UUID | PK |
| id_user | UUID | FK → users (author) |
| title | VARCHAR(200) | Tiêu đề bản tin |
| content | TEXT | Nội dung HTML/Markdown |
| target_audience | VARCHAR(20) | ALL/CUSTOMER/BUSINESS |
| status | VARCHAR(20) | PENDING/APPROVED/REJECTED/EXPIRED |
| is_featured | BOOLEAN | Tin nổi bật (Admin đánh dấu), default FALSE |
| is_approved | BOOLEAN | Duyệt tự động |
| start_date | DATETIME | Ngày bắt đầu hiển thị |
| end_date | DATETIME | Ngày kết thúc |
| created_at | TIMESTAMP | Ngày tạo |
| approved_at | TIMESTAMP | Ngày duyệt |

### MongoDB Collection: `images`
- Lưu images gắn với bulletin (refId → bulletin_board.idBulletin, type = "bulletin")

## 4. API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/bulletin/creation` | Tạo bản tin mới |
| GET | `/bulletin/list` | Danh sách bản tin (5 loại cho Customer) |
| GET | `/bulletin/{id}` | Chi tiết bản tin |
| PATCH | `/bulletin/{id}` | Cập nhật bản tin (idUser trong body) |
| DELETE | `/bulletin/{id}?idUser=` | Xóa bản tin (idUser là query param) |
| PATCH | `/bulletin/{id}/approve` | Admin duyệt bản tin |
| PATCH | `/bulletin/{id}/reject` | Admin từ chối |
| GET | `/bulletin/pending` | Danh sách chờ duyệt (Admin) |
| GET | `/bulletin/my-bulletins` | Danh sách bản tin đã tạo (filter theo status) |

### 5 loại tin hiển tbhị cho Customer

Khi Customer gọi `GET /bulletin/list?audience=CUSTOMER`, response trả về 5 danh sách:

| Key | Mô tả | Logic lấy dữ liệu |
|-----|-------|-------------------|
| `leaderboard` | Tin nổi bật (3 items) | `isFeatured = true`, mới nhất lên đầu, LIMIT 3 |
| `featuredGrid` | Tin nổi bật khác (grid 4 cột) | `isFeatured = true`, exclude leaderboard, mới nhất lên đầu |
| `newestScroll` | Tin mới (horizontal scroll) | Mới nhất (createdAt DESC), chỉ trong 3 ngày, exclude featured |
| `expiringSoon` | Tin sắp hết hạn (grid + icon cảnh báo) | `endDate` trong 5 ngày tới, gần hết nhất lên đầu (endDate ASC) |
| `regularGrid` | Tin thường (grid 3 cột) | Còn lại, mới nhất lên đầu, exclude tất cả trên |

### Request/Response Examples

**POST `/bulletin/creation`**

*Admin tạo (isFeatured = true, auto APPROVED):*
```json
{
  "idUser": "a27efa6d-00ac-4e76-bec9-42d916d284ce",
  "title": "Khuyến mãi mùa hè 2026 - Giảm 50%",
  "content": "<p>Cực kỳ hấp dẫn! Giảm ngay 50% cho tất cả các phòng Premium. Áp dụng từ ngày 01/06 đến 30/08/2026.</p>",
  "targetAudience": "CUSTOMER",
  "isFeatured": true,
  "startDate": "2026-05-01T00:00:00",
  "endDate": "2026-08-30T23:59:59",
  "imageUrl": "https://picsum.photos/seed/bulletin1/800/400"
}
```

*Business tạo (isFeatured bị bỏ qua, luôn = false, status = PENDING):*
```json
{
  "idUser": "c7075719-a0ae-458b-885a-3316db3715ea",
  "title": "Khuyến mãa đặc biệt từ Resort Sunset",
  "content": "<p>Resort Sunset hân hoan chào đón quý khách với gói nghỉ dưỡng 3 ngày 2 đêm chỉ 1.999.000đ.</p>",
  "targetAudience": "CUSTOMER",
  "startDate": "2026-05-18T00:00:00",
  "endDate": "2026-06-18T23:59:59",
  "imageUrl": "https://picsum.photos/seed/bulletin5/800/400"
}
```

*Response (Admin tạo):*
```json
{
  "code": "200",
  "message": "Tạo bản tin thành công!",
  "data": {
    "idBulletin": "uuid-bulletin",
    "title": "Khuyến mãi mùa hè 2026 - Giảm 50%",
    "content": "<p>Cực kỳ hấp dẫn!...</p>",
    "targetAudience": "CUSTOMER",
    "status": "APPROVED",
    "isFeatured": true,
    "isApproved": true,
    "startDate": "2026-05-01T00:00:00",
    "endDate": "2026-08-30T23:59:59",
    "createdAt": "2026-05-19T10:00:00",
    "approvedAt": "2026-05-19T10:00:00",
    "author": {
      "idUser": "a27efa6d-00ac-4e76-bec9-42d916d284ce",
      "firstName": "Admin",
      "lastName": "System",
      "email": "admin@example.com"
    },
    "images": ["https://picsum.photos/seed/bulletin1/800/400"]
  }
}
```

*Response (Business tạo):*
```json
{
  "code": "200",
  "message": "Tạo bản tin thành công!",
  "data": {
    "idBulletin": "uuid-bulletin",
    "title": "Khuyến mãa đặc biệt từ Resort Sunset",
    "content": "<p>Resort Sunset...</p>",
    "targetAudience": "CUSTOMER",
    "status": "PENDING",
    "isFeatured": false,
    "isApproved": false,
    "startDate": "2026-05-18T00:00:00",
    "endDate": "2026-06-18T23:59:59",
    "createdAt": "2026-05-19T11:00:00",
    "approvedAt": null,
    "author": {
      "idUser": "c7075719-a0ae-458b-885a-3316db3715ea",
      "firstName": "Business",
      "lastName": "Owner",
      "email": "business@example.com"
    },
    "images": ["https://picsum.photos/seed/bulletin5/800/400"]
  }
}
```

**GET `/bulletin/list?audience=CUSTOMER`**
```json
{
  "code": "200",
  "message": "Lấy danh sách bản tin thành công!",
  "data": {
    "leaderboard": [
      { "idBulletin": "uuid-1", "title": "Tin nổi bật 1", "isFeatured": true, "createdAt": "2026-05-19T...", "..." },
      { "idBulletin": "uuid-2", "title": "Tin nổi bật 2", "isFeatured": true, "createdAt": "2026-05-18T...", "..." },
      { "idBulletin": "uuid-3", "title": "Tin nổi bật 3", "isFeatured": true, "createdAt": "2026-05-17T...", "..." }
    ],
    "featuredGrid": [
      { "idBulletin": "uuid-4", "title": "Tin nổi bật khác", "isFeatured": true, "createdAt": "2026-05-16T...", "..." }
    ],
    "newestScroll": [
      { "idBulletin": "uuid-5", "title": "Tin mới nhất", "createdAt": "2026-05-19T...", "..." }
    ],
    "expiringSoon": [
      { "idBulletin": "uuid-6", "title": "Tin sắp hết hạn", "endDate": "2026-05-22T...", "..." }
    ],
    "regularGrid": [
      { "idBulletin": "uuid-7", "title": "Tin thường", "createdAt": "2026-05-15T...", "..." }
    ]
  }
}
```

**PATCH `/bulletin/{id}`**
```json
{
  "idUser": "a27efa6d-00ac-4e76-bec9-42d916d284ce",
  "title": "Khuyến mãa mùa hè - cập nhật",
  "content": "<p>Giảm 40% cho tất cả phòng.</p>",
  "isFeatured": true,
  "imageUrl": "https://picsum.photos/seed/newimage/800/400"
}
```

**DELETE `/bulletin/{id}?idUser=uuid-cua-user`**
```json
{
  "code": "200",
  "message": "Xóa bản tin thành công!"
}
```

## 5. Phân quyền

| Action | Author (Business) | Admin |
|--------|-------------------|-------|
| Tạo bản tin | ✓ (PENDING, isFeatured=false) | ✓ (APPROVED, isFeatured=optional) |
| Xem danh sách | ✓ | ✓ |
| Xem chi tiết | ✓ | ✓ |
| Cập nhật bản tin | ✓ (chỉ bản tin chưa duyệt) | ✓ (tất cả, bao gồm isFeatured) |
| Xóa bản tin | ✓ | ✓ |
| Duyệt bản tin | ✗ | ✓ |
| Từ chối bản tin | ✗ | ✓ |
| Đánh dấu isFeatured | ✗ | ✓ |

**Lưu ý:**
- `idUser` được truyền trực tiếp trong request body (PATCH) hoặc query param (DELETE)
- Chỉ Admin mới có quyền thay đổi `isFeatured`
- Business tạo bản tin luôn có `isFeatured = false`, không thể thay đổi

## 6. Event tích hợp
- Khi Admin approve bulletin của Business → gửi notification **type = SYSTEM** đến business owner
- Khi Admin reject bulletin của Business → gửi notification **type = SYSTEM** với lý do từ chối

## 7. Database
- **PostgreSQL**: Lưu bulletin chính (relationships, transactions)
- **MongoDB**: `images` collection cho images (refId → bulletin, type = "bulletin")

## 8. SQL Script tạo bảng

```sql
-- =====================================================
-- Bulletin Board Table
-- =====================================================
CREATE TABLE IF NOT EXISTS bulletin_board (
    id_bulletin UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_user UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    target_audience VARCHAR(20) DEFAULT 'CUSTOMER',
    status VARCHAR(20) DEFAULT 'PENDING',
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    is_approved BOOLEAN NOT NULL DEFAULT FALSE,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,

    CONSTRAINT fk_bulletin_user FOREIGN KEY (id_user)
        REFERENCES users(id_user) ON DELETE CASCADE,
    CONSTRAINT chk_target_audience CHECK (target_audience IN ('ALL', 'CUSTOMER', 'BUSINESS')),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED'))
);

-- Index cho các truy vấn phân loại tin
CREATE INDEX idx_bulletin_status_featured ON bulletin_board(status, is_featured);
CREATE INDEX idx_bulletin_audience_status ON bulletin_board(target_audience, status);
CREATE INDEX idx_bulletin_end_date ON bulletin_board(end_date) WHERE end_date IS NOT NULL;
CREATE INDEX idx_bulletin_user_status ON bulletin_board(id_user, status);

-- Migration: Xóa cột priority nếu bảng đã tồn tại
-- ALTER TABLE bulletin_board DROP COLUMN IF EXISTS priority;
-- ALTER TABLE bulletin_board ADD COLUMN is_featured BOOLEAN NOT NULL DEFAULT FALSE;
```

## 9. Sample Data (10 bản tin)

**Admin ID:** `a27efa6d-00ac-4e76-bec9-42d916d284ce`
**Business ID:** `c7075719-a0ae-458b-885a-3316db3715ea`

### Admin tạo (5 bản tin - APPROVED)

```json
// 1. Tin nổi bật
{
  "idUser": "a27efa6d-00ac-4e76-bec9-42d916d284ce",
  "title": "Khuyến mãi mùa hè 2026 - Giảm 50%",
  "content": "<p>Cực kỳ hấp dẫn! Giảm ngay 50% cho tất cả các phòng Premium.</p>",
  "targetAudience": "CUSTOMER",
  "isFeatured": true,
  "startDate": "2026-05-01T00:00:00",
  "endDate": "2026-08-30T23:59:59",
  "imageUrl": "https://picsum.photos/seed/bulletin1/800/400"
}

// 2. Tin nổi bật
{
  "idUser": "a27efa6d-00ac-4e76-bec9-42d916d284ce",
  "title": "Grand Opening - Khách sạn Ocean View",
  "content": "<p>Chào mừng khai trương Khách sạn Ocean View với nhiều ưu đãi đặc biệt.</p>",
  "targetAudience": "ALL",
  "isFeatured": true,
  "startDate": "2026-05-10T00:00:00",
  "endDate": "2026-06-10T23:59:59",
  "imageUrl": "https://picsum.photos/seed/bulletin2/800/400"
}

// 3. Tin nổi bật
{
  "idUser": "a27efa6d-00ac-4e76-bec9-42d916d284ce",
  "title": "Tuần lễ vàng - Ưu đãi x3 cho thành viên VIP",
  "content": "<p>Nhân dịp tuần lễ vàng, tất cả thành viên VIP được nhận ưu đãi x3 điểm tích lũy.</p>",
  "targetAudience": "CUSTOMER",
  "isFeatured": true,
  "startDate": "2026-05-01T00:00:00",
  "endDate": "2026-05-31T23:59:59",
  "imageUrl": "https://picsum.photos/seed/bulletin3/800/400"
}

// 4. Tin thường
{
  "idUser": "a27efa6d-00ac-4e76-bec9-42d916d284ce",
  "title": "Combo du lịch tiết kiệm - Bay + Khách sạn",
  "content": "<p>Chỉ từ 2.999.000đ, bạn đã có trọn gói vé máy bay khứ hồi + 2 đêm khách sạn 4 sao.</p>",
  "targetAudience": "ALL",
  "isFeatured": false,
  "startDate": "2026-05-05T00:00:00",
  "endDate": "2026-09-05T23:59:59",
  "imageUrl": "https://picsum.photos/seed/bulletin4/800/400"
}

// 5. Tin thường
{
  "idUser": "a27efa6d-00ac-4e76-bec9-42d916d284ce",
  "title": "Hướng dẫn đặt phòng - Nhận ưu đãi thêm",
  "content": "<p>Hướng dẫn đặt phòng đúng cách để nhận thêm 10% giảm giá.</p>",
  "targetAudience": "ALL",
  "isFeatured": false,
  "startDate": "2026-04-15T00:00:00",
  "endDate": "2026-12-31T23:59:59",
  "imageUrl": "https://picsum.photos/seed/bulletin9/800/400"
}
```

### Business tạo (5 bản tin - PENDING)

```json
// 6. Tin chờ duyệt
{
  "idUser": "c7075719-a0ae-458b-885a-3316db3715ea",
  "title": "Khuyến mãa đặc biệt từ Resort Sunset",
  "content": "<p>Resort Sunset hân hoan chào đón quý khách với gói nghỉ dưỡng 3 ngày 2 đêm chỉ 1.999.000đ.</p>",
  "targetAudience": "CUSTOMER",
  "startDate": "2026-05-18T00:00:00",
  "endDate": "2026-06-18T23:59:59",
  "imageUrl": "https://picsum.photos/seed/bulletin5/800/400"
}

// 7. Tin chờ duyệt
{
  "idUser": "c7075719-a0ae-458b-885a-3316db3715ea",
  "title": "Mùa hè rực rỡ - Ưu đãi cho gia đình",
  "content": "<p>Chương trình Mùa hè rực rỡ dành cho gia đình: đặt phòng gia đình được giảm 30%.</p>",
  "targetAudience": "CUSTOMER",
  "startDate": "2026-05-19T00:00:00",
  "endDate": "2026-08-31T23:59:59",
  "imageUrl": "https://picsum.photos/seed/bulletin6/800/400"
}

// 8. Tin chờ duyệt
{
  "idUser": "c7075719-a0ae-458b-885a-3316db3715ea",
  "title": "Early Bird - Đặt sớm nhận ưu đãi",
  "content": "<p>Đặt phòng trước 30 ngày được giảm 25%. Áp dụng cho tất cả loại phòng.</p>",
  "targetAudience": "CUSTOMER",
  "startDate": "2026-05-20T00:00:00",
  "endDate": "2026-10-20T23:59:59",
  "imageUrl": "https://picsum.photos/seed/bulletin7/800/400"
}

// 9. Tin chờ duyệt
{
  "idUser": "c7075719-a0ae-458b-885a-3316db3715ea",
  "title": "Weekend Special - Giảm 35% cuối tuần",
  "content": "<p>Chỉ áp dụng Thứ Bảy và Chủ Nhật. Đặt ngay để nhận ưu đãi!</p>",
  "targetAudience": "CUSTOMER",
  "startDate": "2026-05-15T00:00:00",
  "endDate": "2026-08-15T23:59:59",
  "imageUrl": "https://picsum.photos/seed/bulletin8/800/400"
}

// 10. Tin chờ duyệt
{
  "idUser": "c7075719-a0ae-458b-885a-3316db3715ea",
  "title": "Honeymoon Package - Trăng mật giảm 40%",
  "content": "<p>Gói trăng mật sang trọng: phòng Deluxe view biển, champagne, spa. Chỉ 3.599.000đ.</p>",
  "targetAudience": "CUSTOMER",
  "startDate": "2026-05-10T00:00:00",
  "endDate": "2026-12-31T23:59:59",
  "imageUrl": "https://picsum.photos/seed/bulletin10/800/400"
}
```

**Danh sách ảnh mẫu (URL thật, tồn tại):**
| # | URL |
|---|-----|
| 1 | https://picsum.photos/seed/bulletin1/800/400 |
| 2 | https://picsum.photos/seed/bulletin2/800/400 |
| 3 | https://picsum.photos/seed/bulletin3/800/400 |
| 4 | https://picsum.photos/seed/bulletin4/800/400 |
| 5 | https://picsum.photos/seed/bulletin5/800/400 |
| 6 | https://picsum.photos/seed/bulletin6/800/400 |
| 7 | https://picsum.photos/seed/bulletin7/800/400 |
| 8 | https://picsum.photos/seed/bulletin8/800/400 |
| 9 | https://picsum.photos/seed/bulletin9/800/400 |
| 10 | https://picsum.photos/seed/bulletin10/800/400 |

## 10. Implementation Status

### Đã hoàn thành (2026-05-19)

**Files đã tạo:**
| File | Mô tả |
|------|-------|
| `Constant/PredefinedStatusBulletin.java` | Constants: status, targetAudience |
| `Models/BulletinBoard.java` | Entity JPA với đầy đủ fields (isFeatured thay priority) |
| `Repositories/Jpa/BulletinBoardRepository.java` | Repository với JOIN FETCH + 5 query methods cho phân loại tin |
| `Dto/Request/BulletinBoardCreationRequest.java` | Request tạo bản tin (isFeatured thay priority) |
| `Dto/Request/BulletinBoardUpdateRequest.java` | Request cập nhật (isFeatured thay priority) |
| `Dto/Request/BulletinBoardApprovalRequest.java` | Request phê duyệt/từ chối |
| `Dto/Response/BulletinBoardResponse.java` | Response + AuthorInfo + images (isFeatured) |
| `Dto/Response/BulletinBoardListResponse.java` | Response chứa 5 danh sách tin cho Customer |
| `Mappers/BulletinBoardMapper.java` | MapStruct mapper |
| `Services/BulletinBoardService.java` | Logic + notification event + phân loại 5 loại tin |
| `Controllers/BulletinBoardController.java` | REST endpoints |

**Files đã sửa:**
| File | Thay đổi |
|------|----------|
| `Models/BulletinBoard.java` | `priority` → `is_featured` |
| `Dto/Request/BulletinBoardCreationRequest.java` | `Integer priority` → `Boolean isFeatured` |
| `Dto/Request/BulletinBoardUpdateRequest.java` | `Integer priority` → `Boolean isFeatured` |
| `Dto/Response/BulletinBoardResponse.java` | `Integer priority` → `Boolean isFeatured` |
| `Mappers/BulletinBoardMapper.java` | Thêm ignore mapping cho isFeatured |
| `Services/BulletinBoardService.java` | Admin mới được set isFeatured=true |
| `Repositories/Jpa/BulletinBoardRepository.java` | Thêm 5 query methods cho phân loại tin + findFeaturedLeaders dùng native query LIMIT 3 |
| `Controllers/BulletinBoardController.java` | `/list` trả về BulletinBoardListResponse |

**API Endpoints đã implement:**
| Method | Endpoint | Mô tả |
|--------|----------|--------|
| POST | `/bulletin/creation` | Tạo bản tin mới |
| GET | `/bulletin/list` | Danh sách bản tin (5 loại cho Customer) |
| GET | `/bulletin/{id}` | Chi tiết bản tin |
| PATCH | `/bulletin/{id}` | Cập nhật bản tin (Admin chỉnh isFeatured) |
| DELETE | `/bulletin/{id}` | Xóa bản tin |
| GET | `/bulletin/pending` | Danh sách chờ duyệt (Admin) |
| GET | `/bulletin/my-bulletins` | Danh sách bản tin đã tạo (filter theo status) |
| PATCH | `/bulletin/{id}/approve` | Admin duyệt bản tin |
| PATCH | `/bulletin/{id}/reject` | Admin từ chối bản tin |

**Exception handling:**
- `BULLETIN_NOT_FOUND` - Không tìm thấy bản tin
- `BULLETIN_ACCESS_DENIED` - Không có quyền
- `BULLETIN_INVALID_TARGET` - Target audience không hợp lệ
- `BULLETIN_ALREADY_APPROVED` - Đã duyệt rồi
- `BULLETIN_ALREADY_REJECTED` - Đã từ chối rồi
- `BULLETIN_TITLE_REQUIRED` - Tiêu đề trống

**Hoàn thành:**
- Thay `priority` bằng `isFeatured` (Boolean)
- Chỉ Admin mới có quyền đánh dấu `isFeatured = true`
- Business tạo bản tin luôn có `isFeatured = false`
- API `/bulletin/list` trả về 5 danh sách: leaderboard (3 items), featuredGrid, newestScroll, expiringSoon, regularGrid
- `leaderboard` LIMIT 3, sắp xếp mới nhất lên đầu
- `featuredGrid` lấy tất cả isFeatured còn lại (exclude leaderboard)
- `expiringSoon` sắp xếp theo endDate ASC (gần hết hạn nhất lên đầu)
- Phân loại tin tự động exclude để tránh trùng lặp
- 10 sample data với URL ảnh từ Lorem Picsum

**Chưa hoàn thành:**
- Tích hợp JWT authentication cho các method update/delete