# 07 — Tạo lịch trình du lịch tự động bằng AI

> **Mới ở Đồ án 2.** Người dùng tạo một "lịch trình" (itinerary) cho chuyến đi, sau đó để AI **tự sinh lịch trình chi tiết theo từng ngày/khung giờ** dựa trên điểm đến, ngân sách và sở thích cá nhân, đồng thời gắn dữ liệu thật từ database (khách sạn, tour, vé, nhà hàng, hoạt động).

## 1. Hai phần chính
1. **Itinerary CRUD** — quản lý lịch trình và các hạng mục (items) thủ công.
2. **AI Scheduler** — sinh tự động nội dung lịch trình bằng pipeline 2 lần gọi LLM.

## 2. Entity
- `Itinerary`: idItinerary, users (FK), destination, startDate/endDate, budget, notes, status (`DRAFT` → `AI_GENERATED` → `CONFIRMED` → ...), totalEstimatedCost.
- `ItineraryItem`: idItem, itinerary (FK), itemType (`flight/accommodation/tour/transport/activity/meal`), scheduledDate, startTime/endTime, suggestedName, suggestedLocation, suggestedPrice, estimatedDurationMinutes, refId (UUID thực thể DB, có thể null), displayOrder.
- `UserPreferences`: sở thích du lịch (travelStyle, budgetLevel, travelingWith, hasChildren, interests, preferredActivities, dietaryRestrictions) — đầu vào cho AI.

## 3. API
**Itinerary** — `/itinerary` (CRUD), `/itinerary/.../items` (`ItineraryItemController`): tạo/sửa/xóa/sắp xếp item.
**Sở thích** — `/user-preferences` (lấy/ghi UserPreferences).
**AI Scheduler** — `POST /ai-scheduler/generate` body `{ idItinerary }`, header `idUser` (gateway inject, kiểm tra sở hữu). Trả về Itinerary đầy đủ items + `suggestedTours`.

## 4. Pipeline AI Scheduler (`AISchedulerService.generateSchedule`)
```
1. Load itinerary (bắt buộc DRAFT) + UserPreferences; kiểm tra itinerary thuộc về idUser.
2. LLM #1: sinh khung lịch trình ngày/khung giờ + khối "needs" (cần accommodation/tour/transport/restaurant/activity?).
3. Truy DB theo needs (SchedulingTools): khách sạn (kèm phòng/dịch vụ), tour theo điểm đến+ngân sách, vé theo tuyến, nhà hàng/hoạt động theo thành phố — đều giới hạn số lượng để tránh tràn context.
4. LLM #2 (enrichment): nhét dữ liệu DB vào prompt → gắn ref_id (UUID thực), giá chuẩn, duration hợp lý.
5. Parse JSON, chỉ chấp nhận ref_id thuộc tập UUID đã fetch (chống bịa); lưu items + tính tổng chi phí trong 1 transaction; set status AI_GENERATED.
6. Gợi ý thêm danh sách tour phù hợp điểm đến/ngân sách.
```
- LLM dùng **Groq `meta-llama/llama-4-scout-17b-16e-instruct`** (chung model với chatbot); có cơ chế retry khi JSON hỏng.
- Có guideline duration thực tế (đêm khách sạn = 24h, máy bay nội địa 1–2h, meal 45–90 phút...).

## 5. Vị trí mã nguồn
`Controllers/ItineraryController.java`, `ItineraryItemController.java`, `AISchedulerController.java`; `Services/ItineraryService.java`, `AISchedulerService.java`, `UserPreferencesService.java`; `Helpers/SchedulingTools.java`; `Models/Itinerary.java`, `ItineraryItem.java`, `UserPreferences.java`. Spec gốc: `Docs/SPEC_ITINERARY.md`, `Docs/SPEC_AI_SCHEDULE.md`.

> Bảo mật: endpoint generate kiểm tra quyền sở hữu itinerary qua header `idUser` (gateway introspect JWT) — tránh truy cập/ghi đè lịch trình của người khác.
