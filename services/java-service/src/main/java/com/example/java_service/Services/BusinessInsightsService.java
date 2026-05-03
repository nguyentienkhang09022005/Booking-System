package com.example.java_service.Services;

import com.example.java_service.Dto.Response.BusinessInsightsResponse;
import com.example.java_service.Models.Booking;
import com.example.java_service.Repositories.Jpa.BookingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BusinessInsightsService {

    private final ChatLanguageModel chatModel;
    private final BookingRepository bookingRepository;
    private final ObjectMapper objectMapper;

    private static final List<String> SEGMENTS = List.of("FAMILY", "COUPLES", "GROUP", "TOURIST", "BUSINESS");

    public BusinessInsightsResponse getInsights(UUID businessId, Integer months) {
        int monthsToUse = months != null && months > 0 ? months : 3;
        LocalDateTime startDate = LocalDateTime.now().minusMonths(monthsToUse);

        List<Booking> bookings = bookingRepository.findByBusinessIdAndDateRange(businessId, startDate, LocalDateTime.now());

        SummaryData summary = buildSummary(bookings);
        List<String> insights = generateInsights(bookings, summary);
        List<BusinessInsightsResponse.RecommendationDTO> recommendations = generateRecommendations(summary);

        return BusinessInsightsResponse.builder()
                .summary(summary.toString())
                .insights(insights)
                .recommendations(recommendations)
                .build();
    }

    public String getAIInsights(UUID businessId, Integer months) {
        int monthsToUse = months != null && months > 0 ? months : 3;
        LocalDateTime startDate = LocalDateTime.now().minusMonths(monthsToUse);

        List<Booking> bookings = bookingRepository.findByBusinessIdAndDateRange(businessId, startDate, LocalDateTime.now());

        SummaryData summary = buildSummary(bookings);
        String summaryJson = buildSummaryForAI(summary);

        String systemPrompt = """
            Bạn là chuyên gia marketing du lịch với 10 năm kinh nghiệm.
            Phân tích data kinh doanh và đưa ra 3 gợi ý chiến lược ngắn gọn, thực tế.
            Trả lời JSON format:
            {
              "summary": "Tóm tắt 1-2 câu về tình hình kinh doanh",
              "insights": ["insight 1", "insight 2", "insight 3"],
              "recommendations": [
                {"segment": "SEGMENT_NAME", "action": "Hành động cụ thể", "priority": "HIGH|MEDIUM|LOW"}
              ]
            }
            """;

        try {
            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(
                            SystemMessage.from(systemPrompt),
                            UserMessage.from("Data kinh doanh: " + summaryJson)
                    ))
                    .build();

            String response = chatModel.chat(request).aiMessage().text();
            return extractJsonFromResponse(response);
        } catch (Exception e) {
            log.error("Error getting AI insights: {}", e.getMessage());
            return "{\"error\": \"Failed to generate AI insights\"}";
        }
    }

    private SummaryData buildSummary(List<Booking> bookings) {
        Map<String, SegmentData> segmentData = new LinkedHashMap<>();
        for (String segment : SEGMENTS) {
            segmentData.put(segment, new SegmentData());
        }

        double totalRevenue = 0;
        int totalBookings = bookings.size();

        for (Booking booking : bookings) {
            if (booking.getTotalPrice() != null) {
                totalRevenue += booking.getTotalPrice().doubleValue();
            }

            String segment = booking.getUsers() != null ? booking.getUsers().getCustomerSegment() : "UNKNOWN";
            if (segment != null && segmentData.containsKey(segment)) {
                SegmentData sd = segmentData.get(segment);
                sd.count++;
                sd.revenue += booking.getTotalPrice() != null ? booking.getTotalPrice().doubleValue() : 0;
            }
        }

        for (SegmentData sd : segmentData.values()) {
            sd.percentage = totalBookings > 0 ? Math.round((double) sd.count / totalBookings * 1000) / 10.0 : 0;
            sd.avgBookingValue = sd.count > 0 ? (int) Math.round(sd.revenue / sd.count) : 0;
        }

        String topSegment = segmentData.entrySet().stream()
                .max(Comparator.comparingDouble(e -> e.getValue().count))
                .map(Map.Entry::getKey)
                .orElse("TOURIST");

        return new SummaryData(totalRevenue, totalBookings, segmentData, topSegment);
    }

    private String buildSummaryForAI(SummaryData summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tổng quan kinh doanh:\n");
        sb.append("- Tổng doanh thu: ").append(Math.round(summary.totalRevenue)).append(" VND\n");
        sb.append("- Tổng booking: ").append(summary.totalBookings).append("\n");
        sb.append("- Giá trị TB/booking: ").append(summary.totalBookings > 0 ? Math.round(summary.totalRevenue / summary.totalBookings) : 0).append(" VND\n");
        sb.append("- Segment hàng đầu: ").append(summary.topSegment).append("\n\n");
        sb.append("Chi tiết theo segment:\n");

        for (Map.Entry<String, SegmentData> entry : summary.segmentData.entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ")
                    .append(entry.getValue().count).append(" bookings (")
                    .append(entry.getValue().percentage).append("%), ")
                    .append(Math.round(entry.getValue().revenue)).append(" VND\n");
        }

        return sb.toString();
    }

    private List<String> generateInsights(List<Booking> bookings, SummaryData summary) {
        List<String> insights = new ArrayList<>();

        if (summary.totalBookings > 0 && summary.topSegment != null) {
            insights.add(String.format("Nhóm %s chiếm tỷ lệ cao nhất (%s%%) trong khách hàng của bạn",
                    summary.topSegment, summary.segmentData.get(summary.topSegment).percentage));
        }

        for (Map.Entry<String, SegmentData> entry : summary.segmentData.entrySet()) {
            if (entry.getKey().equals("BUSINESS") && entry.getValue().count > 0) {
                insights.add(String.format("Nhóm BUSINESS có avg booking value cao nhất: %d VND", entry.getValue().avgBookingValue));
            }

            if (entry.getKey().equals("FAMILY") && entry.getValue().count > 0) {
                insights.add("Nhóm FAMILY ổn định - nên giữ chương trình hiện tại");
            }
        }

        if (insights.isEmpty()) {
            insights.add("Dữ liệu chưa đủ để đưa ra insights chi tiết");
        }

        return insights;
    }

    private List<BusinessInsightsResponse.RecommendationDTO> generateRecommendations(SummaryData summary) {
        List<BusinessInsightsResponse.RecommendationDTO> recommendations = new ArrayList<>();

        SegmentData businessData = summary.segmentData.get("BUSINESS");
        if (businessData != null && businessData.count < 5) {
            recommendations.add(BusinessInsightsResponse.RecommendationDTO.builder()
                    .segment("BUSINESS")
                    .action("Thêm business center, wifi tốc độ cao, meeting rooms để thu hút khách công tác")
                    .priority("HIGH")
                    .build());
        }

        SegmentData familyData = summary.segmentData.get("FAMILY");
        if (familyData != null && familyData.count > 0) {
            recommendations.add(BusinessInsightsResponse.RecommendationDTO.builder()
                    .segment("FAMILY")
                    .action("Tạo family packages với kids activities, giảm giá cho trẻ em")
                    .priority("MEDIUM")
                    .build());
        }

        SegmentData couplesData = summary.segmentData.get("COUPLES");
        if (couplesData != null && couplesData.count > 0) {
            recommendations.add(BusinessInsightsResponse.RecommendationDTO.builder()
                    .segment("COUPLES")
                    .action("Tạo romantic packages, honeymoon deals cho cặp đôi")
                    .priority("MEDIUM")
                    .build());
        }

        return recommendations;
    }

    private String extractJsonFromResponse(String response) {
        if (response == null || response.isBlank()) {
            return "{}";
        }

        int jsonStart = response.indexOf('{');
        int jsonEnd = response.lastIndexOf('}');

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd + 1);
        }

        return "{}";
    }

    private record SummaryData(double totalRevenue, int totalBookings, Map<String, SegmentData> segmentData, String topSegment) {
        @Override
        public String toString() {
            return String.format("Doanh thu: %s VND, %d bookings, Top segment: %s", 
                    Math.round(totalRevenue), totalBookings, topSegment);
        }
    }

    private static class SegmentData {
        int count = 0;
        double revenue = 0;
        double percentage = 0;
        int avgBookingValue = 0;
    }
}