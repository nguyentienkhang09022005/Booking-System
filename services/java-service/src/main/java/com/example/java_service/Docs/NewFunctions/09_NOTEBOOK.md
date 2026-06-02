# 09 — Sổ tay cá nhân (Personal Notebook)

> **Mới ở Đồ án 2.** "Cuốn sổ tay điện tử" cho mỗi khách hàng để ghi chú, lập kế hoạch chuyến đi với trình soạn thảo dạng block (giống Notion). Riêng tư — chỉ chủ sở hữu xem.

## 1. Cấu trúc 3 cấp
```
Notebook (1 cuốn sổ theo chuyến đi/sự kiện)
  └── Note (trang/bài viết)
        └── NoteBlock (khối nội dung)
```

## 2. Entity & bảng (PostgreSQL)
- `notebook`: idNotebook, idUser, title, description, coverImage, **template** (`CUSTOM/TRIP_PLANNER/PACKING_LIST/EXPENSE`), isFavorite, createdAt/updatedAt.
- `note`: idNote, idNotebook, title, displayOrder.
- `note_block`: idBlock, idNote, **blockType**, content, **metadata (JSONB)**, displayOrder.
- Ảnh lưu **MongoDB** (`images`, refId→notebook, type="notebook").

## 3. 8 loại block (`blockType`)
`TEXT` (Markdown) · `IMAGE` · `HEADING` (h1–h3) · `LIST` (bullet/numbered) · `CHECKLIST` (checkbox) · `QUOTE` · `LINK` · `TABLE`. `metadata` lưu thông tin phụ (headingLevel, listType, items checklist, imageUrl, columns bảng...).

## 4. Tính năng
Rich text, upload ảnh, **kéo–thả sắp xếp** note/block, template dựng sẵn, **tìm kiếm trong sổ tay**, đánh dấu yêu thích, quyền riêng tư theo owner.

## 5. API — base path `/notebook`
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/` · GET `/list?idUser=` · GET `/{id}?idUser=` | CRUD notebook |
| PATCH | `/{id}` · DELETE `/{id}?idUser=` | Cập nhật / xóa notebook |
| POST | `/{id}/note` · PATCH `/{noteId}/note/{id}` · DELETE `/{notebookId}/note/{id}?idUser=` | CRUD note |
| POST | `/{noteId}/block` · PATCH `/block/{id}` · DELETE `/block/{id}?idUser=` | CRUD block |
| PATCH | `/{id}/reorder` · `/{noteId}/reorder-blocks` | Sắp xếp note/block |
| GET | `/search?idUser=&query=` | Tìm kiếm trong sổ tay |

## 6. Phân quyền
Chỉ **owner** thao tác trên notebook của mình (`NOTEBOOK_ACCESS_DENIED` nếu vi phạm). `idUser` truyền trong body (PATCH/POST) hoặc query param (GET/DELETE).

## 7. Vị trí mã nguồn
`Models/Notebook.java`, `Note.java`, `NoteBlock.java`; `Repositories/Jpa/NotebookRepository.java`, `NoteRepository.java`, `NoteBlockRepository.java`; `Services/NotebookService.java`; `Controllers/NotebookController.java`; `Constant/PredefinedNotebookStatus.java`. Spec gốc: `Docs/FEATURE_02_PERSONAL_NOTEBOOK.md`, SQL: `Docs/SCRIPT_02_PERSONAL_NOTEBOOK.sql`.
