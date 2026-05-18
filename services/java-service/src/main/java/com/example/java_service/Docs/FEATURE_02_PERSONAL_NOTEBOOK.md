# Personal Notebook - Sổ Tay Cá Nhân

## 1. Mục đích
Cho phép khách hàng lưu trữ thông tin cá nhân, ghi chú du lịch riêng tư với đầy đủ công cụ soạn thảo: text, images, rich formatting. Tạo trải nghiệm "cuốn sổ tay điện tử" về chuyến đi.

## 2. Mô tả nghiệp vụ

### Cấu trúc Notebook
- **Notebook**: Đại diện 1 cuốn sổ tay (theo trip/event)
- **Note**: Các trang/bài viết trong sổ tay
- **NoteBlock**: Khối nội dung trong mỗi note (paragraph, image, list, etc.)

### Loại NoteBlock
- `TEXT` - Đoạn văn bản (hỗ trợ Markdown)
- `IMAGE` - Hình ảnh (lưu vào MongoDB images collection)
- `HEADING` - Tiêu đề (h1, h2, h3)
- `LIST` - Danh sách (bullet, numbered)
- `CHECKLIST` - Checklist với checkbox
- `QUOTE` - Trích dẫn
- `LINK` - Liên kết
- `TABLE` - Bảng dữ liệu

### Tính năng
- **Rich text editing**: Markdown-style formatting
- **Image upload**: Lưu vào MongoDB images collection
- **Drag & drop reorder**: Sắp xếp blocks
- **Privacy**: Chỉ chủ sở hữu thấy
- **Templates**: Mẫu có sẵn (Trip Planner, Packing List, Expense Tracker)
- **Search**: Tìm kiếm trong notebook

## 3. Database Design

### PostgreSQL Table: `notebook`

| Column | Type | Description |
|--------|------|-------------|
| id_notebook | UUID | PK |
| id_user | UUID | FK → users |
| title | VARCHAR(200) | Tên cuốn sổ tay |
| description | TEXT | Mô tả |
| cover_image | VARCHAR(500) | URL ảnh bìa |
| template | VARCHAR(50) | CUSTOM/TRIP_PLANNER/PACKING_LIST/EXPENSE |
| is_favorite | BOOLEAN | Đánh dấu yêu thích |
| created_at | TIMESTAMP | Ngày tạo |
| updated_at | TIMESTAMP | Ngày cập nhật |

### PostgreSQL Table: `note`

| Column | Type | Description |
|--------|------|-------------|
| id_note | UUID | PK |
| id_notebook | UUID | FK → notebook |
| title | VARCHAR(200) | Tiêu đề trang |
| display_order | INT | Thứ tự hiển thị |
| created_at | TIMESTAMP | Ngày tạo |
| updated_at | TIMESTAMP | Ngày cập nhật |

### PostgreSQL Table: `note_block`

| Column | Type | Description |
|--------|------|-------------|
| id_block | UUID | PK |
| id_note | UUID | FK → note |
| block_type | VARCHAR(20) | TEXT/IMAGE/HEADING/LIST/CHECKLIST/QUOTE/LINK/TABLE |
| content | TEXT | Nội dung text |
| metadata | JSONB | Extra data (heading_level, list_type, imageUrl từ MongoDB, etc.) |
| display_order | INT | Thứ tự trong note |
| created_at | TIMESTAMP | Ngày tạo |

### MongoDB Collection: `images`
- Lưu images gắn với notebook (refId → notebook.id, type = "notebook")

## 4. API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/notebook` | Tạo notebook mới |
| GET | `/notebook/list?idUser=` | Danh sách notebook của user |
| GET | `/notebook/{id}?idUser=` | Chi tiết notebook (full content) |
| PATCH | `/notebook/{id}` | Cập nhật notebook (idUser trong body) |
| DELETE | `/notebook/{id}?idUser=` | Xóa notebook |
| POST | `/notebook/{id}/note` | Thêm note mới |
| PATCH | `/notebook/{noteId}/note/{id}` | Cập nhật note (idUser trong body) |
| DELETE | `/notebook/{notebookId}/note/{id}?idUser=` | Xóa note |
| POST | `/notebook/{noteId}/block` | Thêm block mới |
| PATCH | `/notebook/block/{id}` | Cập nhật block (idUser trong body) |
| DELETE | `/notebook/block/{id}?idUser=` | Xóa block |
| PATCH | `/notebook/{id}/reorder` | Sắp xếp lại notes |
| PATCH | `/notebook/{noteId}/reorder-blocks` | Sắp xếp blocks |
| GET | `/notebook/search?idUser=&query=` | Tìm kiếm trong notebook |

## 5. Sample Data

### Notebook Creation
```json
{
  "idUser": "c1a0626c-495e-45a7-8138-10bc487fa6d1",
  "title": "Chuyến đi Đà Lạt 2026",
  "description": "Hành trình khám phá Đà Lạt mùa hoa anh đào - 3 ngày 2 đêm",
  "coverImage": "https://happytourvn.com/public/userfiles/images/dalat-cover.jpg",
  "template": "TRIP_PLANNER"
}
```

### Note Creation
```json
{
  "idUser": "c1a0626c-495e-45a7-8138-10bc487fa6d1",
  "title": "Ngày 1 - Khám phá Đà Lạt",
  "displayOrder": 0
}
```

### Block Samples

**TEXT Block:**
```json
{
  "idUser": "c1a0626c-495e-45a7-8138-10bc487fa6d1",
  "blockType": "TEXT",
  "content": "Hôm nay chúng tôi khởi hành từ TP.HCM lúc 6h sáng, đến Đà Lạt lúc 10h30. Thời tiết mát mẻ, 18 độ C.",
  "metadata": {},
  "displayOrder": 0
}
```

**HEADING Block:**
```json
{
  "idUser": "c1a0626c-495e-45a7-8138-10bc487fa6d1",
  "blockType": "HEADING",
  "content": "📍 Địa điểm tham quan",
  "metadata": {"headingLevel": 2},
  "displayOrder": 1
}
```

**LIST Block:**
```json
{
  "idUser": "c1a0626c-495e-45a7-8138-10bc487fa6d1",
  "blockType": "LIST",
  "content": "Hồ Tuyền Trang - Check-in hoa Anh Đào\nThác Datanla - Trải nghiệm máng trượt\nChợ đêm Đà Lạt - Mua đặc sản\nHồ Xuân Hương - Dạo bộ buổi tối",
  "metadata": {"listType": "bullet"},
  "displayOrder": 2
}
```

**CHECKLIST Block:**
```json
{
  "idUser": "c1a0626c-495e-45a7-8138-10bc487fa6d1",
  "blockType": "CHECKLIST",
  "content": "✓ Đặt khách sạn\n✓ Book vé máy bay\n✗ Chuẩn bị vali\n✗ Gửi mail xin nghỉ phép",
  "metadata": {
    "items": [
      {"text": "Đặt khách sạn", "checked": true},
      {"text": "Book vé máy bay", "checked": true},
      {"text": "Chuẩn bị vali", "checked": false},
      {"text": "Gửi mail xin nghỉ phép", "checked": false}
    ]
  },
  "displayOrder": 3
}
```

**IMAGE Block:**
```json
{
  "idUser": "c1a0626c-495e-45a7-8138-10bc487fa6d1",
  "blockType": "IMAGE",
  "content": "Hồ Tuyền Trang buổi sáng",
  "metadata": {
    "imageUrl": "https://happytourvn.com/public/userfiles/images/ho-tuyen-trang.jpg",
    "caption": "Hồ Tuyền Trang - Đà Lạt"
  },
  "displayOrder": 4
}
```

**QUOTE Block:**
```json
{
  "idUser": "c1a0626c-495e-45a7-8138-10bc487fa6d1",
  "blockType": "QUOTE",
  "content": "Đà Lạt mùa nào cũng đẹp, nhưng mùa hoa Anh Đào là tuyệt nhất!",
  "metadata": {"author": "Người dân địa phương"},
  "displayOrder": 5
}
```

**LINK Block:**
```json
{
  "idUser": "c1a0626c-495e-45a7-8138-10bc487fa6d1",
  "blockType": "LINK",
  "content": "Đặt phòng khách sạn Đà Lạt",
  "metadata": {
    "url": "https://happytourvn.com/hotels/dalat",
    "text": "Xem ưu đãi khách sạn Đà Lạt"
  },
  "displayOrder": 6
}
```

**TABLE Block:**
```json
{
  "idUser": "c1a0626c-495e-45a7-8138-10bc487fa6d1",
  "blockType": "TABLE",
  "content": "Ngày | Địa điểm | Chi phí\nDay 1 | Khách sạn Lavender | 800.000đ\nDay 2 | Homestay Hills | 650.000đ\nDay 3 | Resort Dalat Palace | 1.200.000đ",
  "metadata": {
    "columns": ["Ngày", "Địa điểm", "Chi phí"],
    "total": "2.650.000đ"
  },
  "displayOrder": 7
}
```

### Response Examples

**POST `/notebook`**
```json
{
  "code": "200",
  "message": "Tạo sổ tay thành công!",
  "data": {
    "idNotebook": "uuid-notebook",
    "title": "Chuyến đi Đà Lạt 2026",
    "description": "Hành trình khám phá Đà Lạt mùa hoa anh đào",
    "coverImage": "https://happytourvn.com/public/userfiles/images/dalat-cover.jpg",
    "template": "TRIP_PLANNER",
    "isFavorite": false,
    "createdAt": "2026-05-17T10:00:00",
    "updatedAt": "2026-05-17T10:00:00",
    "owner": {
      "idUser": "c1a0626c-495e-45a7-8138-10bc487fa6d1",
      "firstName": "Hoàng",
      "lastName": "Lê Hữu",
      "email": "llhoangvtqt@gmail.com"
    },
    "notes": [],
    "images": ["https://happytourvn.com/public/userfiles/images/dalat-cover.jpg"]
  }
}
```

**POST `/notebook/{id}/note`**
```json
{
  "code": "200",
  "message": "Tạo trang thành công!",
  "data": {
    "idNote": "uuid-note",
    "title": "Ngày 1 - Khám phá Đà Lạt",
    "displayOrder": 0,
    "createdAt": "2026-05-17T10:00:00",
    "updatedAt": "2026-05-17T10:00:00",
    "blocks": []
  }
}
```

**POST `/notebook/{noteId}/block`**
```json
{
  "code": "200",
  "message": "Tạo block thành công!",
  "data": {
    "idBlock": "uuid-block",
    "idNote": "uuid-note",
    "blockType": "TEXT",
    "content": "Hôm nay chúng tôi khởi hành từ TP.HCM...",
    "metadata": {},
    "displayOrder": 0,
    "createdAt": "2026-05-17T10:00:00",
    "images": []
  }
}
```

## 6. Phân quyền

| Action | Owner (User) |
|--------|-------------|
| Tạo notebook | ✓ |
| Xem danh sách | ✓ (chỉ của mình) |
| Xem chi tiết | ✓ (chỉ của mình) |
| Cập nhật notebook | ✓ |
| Xóa notebook | ✓ |
| Tạo note | ✓ |
| Cập nhật note | ✓ |
| Xóa note | ✓ |
| Tạo block | ✓ |
| Cập nhật block | ✓ |
| Xóa block | ✓ |

**Lưu ý:** `idUser` được truyền trực tiếp trong request body (PATCH) hoặc query param (GET/DELETE)

## 7. Database
- **PostgreSQL**: Lưu `notebook`, `note`, `note_block` (relationships, transactions)
- **MongoDB**: `images` collection cho images (refId → notebook.id, type = "notebook")

## 8. Exception Handling

| Error Code | Description |
|------------|-------------|
| `NOTEBOOK_NOT_FOUND` | Không tìm thấy sổ tay |
| `NOTEBOOK_ACCESS_DENIED` | Không có quyền truy cập sổ tay này |
| `NOTEBOOK_TITLE_REQUIRED` | Tiêu đề sổ tay không được để trống |
| `NOTEBOOK_INVALID_TEMPLATE` | Template sổ tay không hợp lệ |
| `NOTE_NOT_FOUND` | Không tìm thấy trang |
| `NOTE_TITLE_REQUIRED` | Tiêu đề trang không được để trống |
| `BLOCK_NOT_FOUND` | Không tìm thấy block |
| `BLOCK_INVALID_TYPE` | Loại block không hợp lệ |

## 9. Implementation Status

### Đã hoàn thành (2026-05-17)

**Files đã tạo:**
| File | Mô tả |
|------|-------|
| `Constant/PredefinedNotebookStatus.java` | Constants: template, blockType |
| `Models/Notebook.java` | Entity JPA notebook |
| `Models/Note.java` | Entity JPA note |
| `Models/NoteBlock.java` | Entity JPA note_block |
| `Repositories/Jpa/NotebookRepository.java` | Repository với JOIN FETCH |
| `Repositories/Jpa/NoteRepository.java` | Repository với JOIN FETCH |
| `Repositories/Jpa/NoteBlockRepository.java` | Repository |
| `Dto/Request/NotebookCreationRequest.java` | Request tạo notebook |
| `Dto/Request/NotebookUpdateRequest.java` | Request cập nhật notebook |
| `Dto/Request/NoteCreationRequest.java` | Request tạo note |
| `Dto/Request/NoteUpdateRequest.java` | Request cập nhật note |
| `Dto/Request/BlockCreationRequest.java` | Request tạo block |
| `Dto/Request/BlockUpdateRequest.java` | Request cập nhật block |
| `Dto/Request/NotesReorderRequest.java` | Request sắp xếp notes |
| `Dto/Request/BlocksReorderRequest.java` | Request sắp xếp blocks |
| `Dto/Response/NotebookResponse.java` | Response với nested NoteDetail, BlockDetail |
| `Dto/Response/NoteResponse.java` | Response cho note |
| `Dto/Response/BlockResponse.java` | Response cho block |
| `Mappers/NotebookMapper.java` | MapStruct mapper |
| `Mappers/NoteMapper.java` | MapStruct mapper |
| `Mappers/NoteBlockMapper.java` | MapStruct mapper |
| `Services/NotebookService.java` | Logic + phân quyền + image handling |
| `Controllers/NotebookController.java` | REST endpoints |
| `Docs/SCRIPT_02_PERSONAL_NOTEBOOK.sql` | Script tạo bảng |

**Files đã sửa:**
| File | Thay đổi |
|------|----------|
| `Exception/ErrorCode.java` | Thêm 8 error codes mới |
| `Models/Notebook.java` | Fix @CreationTimestamp/@UpdateTimestamp columnDefinition |
| `Models/Note.java` | Fix @CreationTimestamp/@UpdateTimestamp columnDefinition |
| `Models/NoteBlock.java` | Fix @CreationTimestamp columnDefinition |
| `Services/NotebookService.java` | Set timestamps manually trong create/update methods |

**API Endpoints đã implement:**
| Method | Endpoint | Mô tả |
|--------|----------|--------|
| POST | `/notebook` | Tạo notebook mới |
| GET | `/notebook/list` | Danh sách notebook của user |
| GET | `/notebook/{id}` | Chi tiết notebook |
| PATCH | `/notebook/{id}` | Cập nhật notebook |
| DELETE | `/notebook/{id}` | Xóa notebook |
| POST | `/notebook/{id}/note` | Tạo note mới |
| PATCH | `/notebook/{noteId}/note/{id}` | Cập nhật note |
| DELETE | `/notebook/{notebookId}/note/{id}` | Xóa note |
| POST | `/notebook/{noteId}/block` | Tạo block mới |
| PATCH | `/notebook/block/{id}` | Cập nhật block |
| DELETE | `/notebook/block/{id}` | Xóa block |
| PATCH | `/notebook/{id}/reorder` | Sắp xếp notes |
| PATCH | `/notebook/{noteId}/reorder-blocks` | Sắp xếp blocks |
| GET | `/notebook/search` | Tìm kiếm notebook |

**Hoàn thành:**
- Truyền `idUser` trực tiếp trong request body/query param
- Hỗ trợ `coverImage` trong notebook (lưu vào MongoDB images)
- Phân quyền: chỉ owner mới được truy cập notebook của mình
- Images lưu vào MongoDB `images` collection (refId → notebook.id, type = "notebook")
- Timestamps (`createdAt`, `updatedAt`) được populate đúng sau khi tạo/cập nhật