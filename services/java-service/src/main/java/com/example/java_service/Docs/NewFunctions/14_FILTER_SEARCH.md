# 14 — Bộ Filter & Search mới

> **Mới ở Đồ án 2.** Thay cho tìm kiếm/lọc đơn giản của Đồ án 1, hệ thống bổ sung **2 bộ API riêng biệt** cho khách hàng, hỗ trợ đồng nhất 4 loại sản phẩm: **Accommodation, Tour, TransportTicket, Voucher**.

## 1. Hai API
### 1.1 Item Filter — `/item-filter`
- Lọc chi tiết theo nhiều tiêu chí cho từng entity: range (min/max giá...), khoảng ngày, thuộc tính.
- **Chỉ trả item đã duyệt** (`isApproved = true` hardcode); không lộ thông tin nhạy cảm (mã voucher, trạng thái duyệt).
- Pagination (page size tối đa 100) + sort; tránh N+1 bằng JOIN FETCH + batch fetch ảnh từ MongoDB.

### 1.2 Item Search — `/item-search`
- Tìm theo từ khóa: case-insensitive `LOWER()` + `LIKE` ở tầng DB.
- **Fuzzy re-ranking** ở tầng Java: thuật toán **Jaro-Winkler + Levenshtein**, **xử lý dấu tiếng Việt** (vd "Ha Noi" khớp "Hà Nội").
- Khi `sortBy=relevance`: xếp hạng theo điểm liên quan (khớp chính xác > khớp có dấu > còn lại nhưng vẫn giữ). Khi sort theo trường khác: không ghi đè lựa chọn của người dùng.

## 2. Endpoint tiêu biểu
```
GET /item-filter/accommodations?city=&minPrice=&maxPrice=&page=&size=&sortBy=
GET /item-filter/tours | /transport-tickets | /vouchers
GET /item-search/accommodations?keyword=Hà Nội&page=&size=&sortBy=relevance
GET /item-search/tours | /transport-tickets | /vouchers
```

## 3. Luồng xử lý
```
Controller (parse + validate)
  → Service (build criteria | gọi search repo)
  → Repository (dynamic WHERE/predicate + JOIN FETCH + Pageable)
  → MongoDB images (batch fetch theo refId)
  → [Search] FuzzySearchService re-rank theo relevance
  → Response DTO (đã phân trang)
```

## 4. Đặc điểm thiết kế
- Tách hoàn toàn khỏi logic cũ (clean architecture), không ảnh hưởng các controller cũ.
- Dùng chung cho FE customer-facing (trang danh sách + thanh tìm kiếm có gợi ý liên quan).

## 5. Vị trí mã nguồn
`Controllers/ItemFilterController.java`, `ItemSearchController.java`; `Services/ItemFilterService.java`, `ItemSearchService.java`, `FuzzySearchService`; Repository filter/search (Jpa). Tài liệu kỹ thuật đầy đủ: `Docs/ITEM_FILTER_SYSTEM.md` (kèm `SPEC_BOOKING_FILTER.md` cho lọc booking).
