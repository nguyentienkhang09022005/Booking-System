# 15 — Phân khúc khách hàng bằng AI (Customer Segmentation)

> **Mới ở Đồ án 2 — bổ sung.** Đây là **engine AI** tự động phân loại từng khách hàng vào một phân khúc dựa trên hành vi & lịch sử đặt chỗ. Nó là tầng **sinh dữ liệu** đứng sau `13_BUSINESS_ANALYTICS.md` (analytics chỉ tổng hợp/hiển thị các phân khúc do engine này tạo ra).

## 1. 5 phân khúc khách hàng
| Segment | Mô tả | Đặc điểm |
|---------|-------|----------|
| `FAMILY` | Du lịch gia đình | Có trẻ nhỏ, đặt nhiều phòng, đặt trước 1–2 tuần, ưu tiên an toàn |
| `COUPLES` | Cặp đôi / honeymoon | 2 người, điểm đến lãng mạn, booking value cao |
| `GROUP` | Du lịch nhóm | >3 người, nhạy giá, hay đặt nhiều tour, lịch linh hoạt |
| `TOURIST` | Khách du lịch phổ thông | Đi lẻ/hỗn hợp, sightseeing, đa dạng loại hình |
| `BUSINESS` | Khách công tác | Short stay, đặt gấp, avg booking value cao nhất |

## 2. Lưu trữ (bảng `users` — cột mới)
| Cột | Ý nghĩa |
|-----|---------|
| `customer_segment` | Phân khúc (1 trong 5) |
| `segment_confidence` | Độ tin cậy 0.0–1.0 |
| `segment_analyzed_at` | Thời điểm phân tích gần nhất |

## 3. Kiến trúc event-driven (tránh circular dependency)
```
UserService / UserPreferencesService / BookingHandleService /
BookingTourService / BookingTransportTicketService
      │  publishEvent(BookingCompletedEvent{userId, bookingType, bookingId})
      ▼
CustomerSegmentationService  @EventListener @Async("segmentationExecutor")
      │  lấy dữ liệu hành vi (UserAnalyticsService) → JSON
      ▼
ICustomerSegmentationAgent (LLM) → trả {segment, confidence, reasoning}
      │  parse + quyết định cập nhật
      ▼
userRepository.updateCustomerSegment(userId, segment, confidence, now)
```
- Dùng **Spring ApplicationEventPublisher** + `@Async` (executor `segmentationExecutor`) → phân tích chạy nền, không chặn luồng đặt chỗ.
- `@Transactional` đặt ở repository (`updateCustomerSegment`) để proxy hoạt động đúng khi gọi từ thread async.

## 4. Logic phân tích thông minh (tiết kiệm gọi AI)
- **Bỏ qua** nếu confidence > 0.8 và chưa "stale" (chưa quá 6 tháng); hoặc confidence > 0.5 và đã phân tích trong 3 tháng gần đây.
- **Chỉ cập nhật** khi segment mới có confidence cao hơn segment cũ ít nhất +0.15 (`RECALCULATE_CONFIDENCE_IMPROVEMENT`).
- Có fallback parse JSON từ text khi LLM trả kèm chữ.

## 5. Liên kết với các phần khác
- Cấp dữ liệu cho **Business Analytics** (`/analytics/segments`, `/insights`, `/insights/ai`).
- Dùng `SegmentationTools` + `UserAnalyticsService` để gom hành vi (booking phòng/tour/vé, sở thích).

## 6. Vị trí mã nguồn
`Services/CustomerSegmentationService.java`, `UserAnalyticsService`; `Services/Interfaces/ICustomerSegmentationAgent.java`; `Helpers/SegmentationTools.java`; `Events/BookingCompletedEvent.java`; `Dto/Request/CustomerSegmentationRequest.java`, `Dto/Response/CustomerSegmentationResponse.java`. Tài liệu kỹ thuật đầy đủ: `Docs/CUSTOMER_SEGMENTATION.md`.
