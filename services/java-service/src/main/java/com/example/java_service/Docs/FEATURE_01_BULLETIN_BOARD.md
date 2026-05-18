# Bulletin Board - Bảng Tin Thông Báo

## 1. Mục đích
Hệ thống bảng tin cho phép Admin và Business tạo nội dung thông báo, tin khuyến mãi hiển thị đến người dùng trong hệ thống với cơ chế duyệt phù hợp.

## 2. Mô tả nghiệp vụ

### Luồng hoạt động
- **Admin tạo bản tin**: Duyệt tự động, chọn target audience (CUSTOMER/BUSINESS/ALL)
- **Business tạo bản tin**: Mặc định target = CUSTOMER, trạng thái PENDING, chờ Admin duyệt mới hiển thị

### Target Audience
- `ALL` - Toàn hệ thống (Admin only)
- `CUSTOMER` - Chỉ khách hàng
- `BUSINESS` - Chỉ đối tác doanh nghiệp (Admin only)

### Trạng thái bản tin
- `PENDING` - Chờ duyệt (Business tạo)
- `APPROVED` - Đã duyệt, hiển thị
- `REJECTED` - Bị từ chối
- `EXPIRED` - Hết hạn

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
| priority | INT | Thứ tự ưu tiên hiển thị |
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
| GET | `/bulletin/list` | Danh sách bản tin (theo audience) |
| GET | `/bulletin/{id}` | Chi tiết bản tin |
| PATCH | `/bulletin/{id}` | Cập nhật bản tin (idUser trong body) |
| DELETE | `/bulletin/{id}?idUser=` | Xóa bản tin (idUser là query param) |
| PATCH | `/bulletin/{id}/approve` | Admin duyệt bản tin |
| PATCH | `/bulletin/{id}/reject` | Admin từ chối |
| GET | `/bulletin/pending` | Danh sách chờ duyệt (Admin) |

### Request/Response Examples

**POST `/bulletin/creation`**
```json
// Request
{
  "idUser": "uuid-cua-user",
  "title": "Khuyến mãa mùa hè",
  "content": "Giảm 30% cho tất cả phòng",
  "targetAudience": "CUSTOMER",
  "priority": 1,
  "startDate": "2026-05-20T00:00:00",
  "endDate": "2026-06-20T23:59:59",
  "imageUrl": "https://example.com/image.jpg"
}

// Response
{
  "code": "200",
  "message": "Tạo bản tin thành công!",
  "data": {
    "idBulletin": "uuid-bulletin",
    "title": "Khuyến mãa mùa hè",
    "content": "Giảm 30% cho tất cả phòng",
    "targetAudience": "CUSTOMER",
    "status": "APPROVED",  // ADMIN tạo -> auto approved
    "priority": 1,
    "isApproved": true,
    "startDate": "2026-05-20T00:00:00",
    "endDate": "2026-06-20T23:59:59",
    "createdAt": "2026-05-17T10:00:00",
    "approvedAt": "2026-05-17T10:00:00",
    "author": {
      "idUser": "uuid-cua-user",
      "firstName": "Nguyễn",
      "lastName": "Văn A",
      "email": "user@example.com"
    },
    "images": []
  }
}
```

**GET `/bulletin/list?audience=CUSTOMER`**
```json
{
  "code": "200",
  "message": "Lấy danh sách bản tin thành công!",
  "data": [...]
}
```

**PATCH `/bulletin/{id}`**
```json
// Request (idUser trong body - author hoặc admin)
{
  "idUser": "uuid-cua-user",
  "title": "Khuyến mãa mùa hè - cập nhật",
  "content": "Giảm 40% cho tất cả phòng",
  "priority": 2,
  "imageUrl": "https://example.com/new-image.jpg"
}

// Response
{
  "code": "200",
  "message": "Cập nhật bản tin thành công!",
  "data": { ... }
}
```

**DELETE `/bulletin/{id}?idUser=`**
```
DELETE /bulletin/{id}?idUser=uuid-cua-user

// Response
{
  "code": "200",
  "message": "Xóa bản tin thành công!"
}
```

## 5. Phân quyền

| Action | Author | Admin |
|--------|--------|-------|
| Tạo bản tin | ✓ (PENDING) | ✓ (APPROVED) |
| Xem danh sách | ✓ | ✓ |
| Xem chi tiết | ✓ | ✓ |
| Cập nhật bản tin | ✓ (chỉ bản tin chưa duyệt) | ✓ (tất cả) |
| Xóa bản tin | ✓ | ✓ |
| Duyệt bản tin | ✗ | ✓ |
| Từ chối bản tin | ✗ | ✓ |

**Lưu ý:** `idUser` được truyền trực tiếp trong request body (PATCH) hoặc query param (DELETE)

## 6. Event tích hợp
- Khi Admin approve bulletin của Business → gửi notification **type = SYSTEM** đến business owner
- Khi Admin reject bulletin của Business → gửi notification **type = SYSTEM** với lý do từ chối

## 7. Database
- **PostgreSQL**: Lưu bulletin chính (relationships, transactions)
- **MongoDB**: `images` collection cho images (refId → bulletin, type = "bulletin")

## 8. Implementation Status

### Đã hoàn thành (2026-05-17)

**Files đã tạo:**
| File | Mô tả |
|------|-------|
| `Constant/PredefinedStatusBulletin.java` | Constants: status, targetAudience |
| `Models/BulletinBoard.java` | Entity JPA với đầy đủ fields |
| `Repositories/Jpa/BulletinBoardRepository.java` | Repository với JOIN FETCH (tránh N+1) |
| `Dto/Request/BulletinBoardCreationRequest.java` | Request tạo bản tin (có `idUser`, `imageUrl`) |
| `Dto/Request/BulletinBoardUpdateRequest.java` | Request cập nhật (có `idUser`, `imageUrl`) |
| `Dto/Request/BulletinBoardApprovalRequest.java` | Request phê duyệt/từ chối |
| `Dto/Response/BulletinBoardResponse.java` | Response + AuthorInfo + images |
| `Mappers/BulletinBoardMapper.java` | MapStruct mapper |
| `Services/BulletinBoardService.java` | Logic + notification event + phân quyền |
| `Controllers/BulletinBoardController.java` | REST endpoints |
| `SCRIPT_01_BULLETIN_BOARD.sql` | Script tạo bảng |

**Files đã sửa:**
| File | Thay đổi |
|------|----------|
| `Exception/ErrorCode.java` | Thêm 7 error codes mới |

**API Endpoints đã implement:**
| Method | Endpoint | Mô tả |
|--------|----------|--------|
| POST | `/bulletin/creation` | Tạo bản tin mới |
| GET | `/bulletin/list` | Danh sách bản tin active |
| GET | `/bulletin/{id}` | Chi tiết bản tin |
| PATCH | `/bulletin/{id}` | Cập nhật bản tin |
| DELETE | `/bulletin/{id}` | Xóa bản tin |
| GET | `/bulletin/pending` | Danh sách chờ duyệt (Admin) |
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
- Truyền `idUser` trực tiếp trong request body/query param
- Hỗ trợ `imageUrl` trong request body khi tạo/update bản tin
- Phân quyền: author và admin đều có thể update/delete

**Chưa hoàn thành:**
- Tích hợp JWT authentication cho các method update/delete