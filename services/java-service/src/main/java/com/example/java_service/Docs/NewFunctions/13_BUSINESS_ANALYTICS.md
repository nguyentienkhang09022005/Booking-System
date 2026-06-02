# 13 — Business Analytics (Phân tích kinh doanh)

> **Mới ở Đồ án 2.** Đồ án 1 chỉ có báo cáo doanh thu (thu–chi). Đồ án 2 bổ sung **phân tích khách hàng** cho doanh nghiệp: phân khúc khách theo mô hình **RFM** và sinh **insight bằng AI**.

## 1. Hai loại phân tích
1. **Phân khúc khách hàng (RFM)** — gom nhóm khách dựa trên Recency (lần gần nhất), Frequency (tần suất), Monetary (chi tiêu) trong một khoảng thời gian (mặc định 6 tháng).
2. **Insight kinh doanh** — tổng hợp chỉ số + **gợi ý hành động bằng AI (Gemini)** dựa trên dữ liệu phân khúc (mặc định 3 tháng).

## 2. API — base path `/analytics`
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/segments?businessId=&months=6` | Phân bố các phân khúc khách hàng |
| GET | `/segments/chart?businessId=&months=6` | Dữ liệu biểu đồ phân khúc |
| GET | `/segments/{segment}?businessId=&months=6` | Chi tiết một phân khúc |
| GET | `/insights?businessId=&months=3` | Insight tổng hợp |
| GET | `/insights/ai?businessId=&months=3` | Insight diễn giải bằng AI (Gemini) |

## 3. Thành phần
- `BusinessAnalyticsService`: tính phân bố phân khúc (`getSegmentDistribution`), dữ liệu biểu đồ (`getChartData`).
- `BusinessInsightsService`: `getInsights` (chỉ số) và `getAIInsights` (gọi Gemini sinh diễn giải/đề xuất).
- DTO: `SegmentDistributionResponse`, `SegmentChartResponse`, `SegmentDetailResponse`, `BusinessInsightsResponse`.

## 4. Ý nghĩa nghiệp vụ
Giúp chủ doanh nghiệp biết nhóm khách VIP / có nguy cơ rời bỏ / khách mới..., từ đó ra quyết định khuyến mãi, chăm sóc. Phần phân khúc RFM cũng liên quan tới luồng cập nhật segment bất đồng bộ (xem tài liệu kỹ thuật).

## 5. Vị trí mã nguồn
`Controllers/BusinessAnalyticsController.java`; `Services/BusinessAnalyticsService.java`, `BusinessInsightsService.java`. Tài liệu kỹ thuật đầy đủ (RFM, thuật toán, fix transaction): `Docs/CUSTOMER_SEGMENTATION.md`.
