package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedBudgetLevel;
import com.example.java_service.Constant.PredefinedItemType;
import com.example.java_service.Constant.PredefinedPaymentStatus;
import com.example.java_service.Constant.PredefinedStatusItinerary;
import com.example.java_service.Constant.PredefinedStatusItineraryItem;
import com.example.java_service.Constant.PredefinedTravelStyle;
import com.example.java_service.Constant.PredefinedTravelingWith;
import com.example.java_service.Dto.Response.ItineraryResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Models.Itinerary;
import com.example.java_service.Models.ItineraryItem;
import com.example.java_service.Models.UserPreferences;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AISchedulerService {

    ChatLanguageModel chatLanguageModel;
    UserPreferencesService userPreferencesService;
    ItineraryService itineraryService;
    ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final Set<String> VALID_ITEM_TYPES = new HashSet<>(Arrays.asList(
            PredefinedItemType.FLIGHT,
            PredefinedItemType.HOTEL,
            PredefinedItemType.TOUR,
            PredefinedItemType.TRANSPORT,
            PredefinedItemType.ACTIVITY,
            PredefinedItemType.MEAL
    ));

    private static final Map<String, String> ITEM_TYPE_ALIASES = new HashMap<>();
    static {
        ITEM_TYPE_ALIASES.put("car", PredefinedItemType.TRANSPORT);
        ITEM_TYPE_ALIASES.put("taxi", PredefinedItemType.TRANSPORT);
        ITEM_TYPE_ALIASES.put("bus", PredefinedItemType.TRANSPORT);
        ITEM_TYPE_ALIASES.put("train", PredefinedItemType.TRANSPORT);
        ITEM_TYPE_ALIASES.put("airplane", PredefinedItemType.FLIGHT);
        ITEM_TYPE_ALIASES.put("plane", PredefinedItemType.FLIGHT);
        ITEM_TYPE_ALIASES.put("flight", PredefinedItemType.FLIGHT);
        ITEM_TYPE_ALIASES.put("hotel", PredefinedItemType.HOTEL);
        ITEM_TYPE_ALIASES.put("homestay", PredefinedItemType.HOTEL);
        ITEM_TYPE_ALIASES.put("resort", PredefinedItemType.HOTEL);
        ITEM_TYPE_ALIASES.put("restaurant", PredefinedItemType.MEAL);
        ITEM_TYPE_ALIASES.put("food", PredefinedItemType.MEAL);
        ITEM_TYPE_ALIASES.put("sightseeing", PredefinedItemType.ACTIVITY);
        ITEM_TYPE_ALIASES.put("visit", PredefinedItemType.ACTIVITY);
        ITEM_TYPE_ALIASES.put("tour", PredefinedItemType.TOUR);
    }

    public ItineraryResponse generateSchedule(String idItinerary) {
        Itinerary itinerary = itineraryService.getItineraryEntity(idItinerary);

        validateItineraryForGeneration(itinerary);

        UserPreferences preferences = userPreferencesService.getPreferencesEntityOrDefault(
                itinerary.getUsers().getIdUser().toString());

        String prompt = buildPrompt(itinerary, preferences);

        String jsonResponse;
        try {
            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(
                            SystemMessage.from(prompt),
                            UserMessage.from("Hãy tạo lịch trình chi tiết theo yêu cầu trên.")
                    ))
                    .build();

            jsonResponse = chatLanguageModel.chat(request).aiMessage().text();

            jsonResponse = jsonResponse.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*$", "");
            jsonResponse = jsonResponse.trim();
            if (!jsonResponse.startsWith("{")) {
                int start = jsonResponse.indexOf("{");
                int end = jsonResponse.lastIndexOf("}");
                if (start != -1 && end != -1) {
                    jsonResponse = jsonResponse.substring(start, end + 1);
                }
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi gọi AI: {}", e.getMessage());
            throw new ApiException(ErrorCode.ITINERARY_CANNOT_GENERATE);
        }

        try {
            itineraryService.deleteItineraryItemsByItineraryId(itinerary.getIdItinerary());

            List<ItineraryItem> items = parseAndSaveItems(jsonResponse, itinerary);

            BigDecimal totalCost = items.stream()
                    .map(ItineraryItem::getSuggestedPrice)
                    .filter(price -> price != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            itinerary.setTotalEstimatedCost(totalCost);
            itinerary.setStatus(PredefinedStatusItinerary.AI_GENERATED);
            itineraryService.updateItineraryStatus(idItinerary, PredefinedStatusItinerary.AI_GENERATED);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi parse và lưu lịch trình: {}", e.getMessage());
            throw new ApiException(ErrorCode.ITINERARY_CANNOT_GENERATE);
        }

        return itineraryService.getItinerary(idItinerary);
    }

    private void validateItineraryForGeneration(Itinerary itinerary) {
        if (itinerary == null) {
            throw new ApiException(ErrorCode.ITINERARY_NOT_FOUND);
        }

        if (!PredefinedStatusItinerary.DRAFT.equals(itinerary.getStatus())) {
            log.warn("Itinerary status không phải DRAFT, hiện tại: {}", itinerary.getStatus());
            throw new ApiException(ErrorCode.ITINERARY_NOT_IN_DRAFT_STATUS);
        }

        if (itinerary.getStartDate() == null || itinerary.getEndDate() == null) {
            throw new ApiException(ErrorCode.INVALID_ITINERARY_DATE);
        }

        if (itinerary.getStartDate().isAfter(itinerary.getEndDate())) {
            throw new ApiException(ErrorCode.INVALID_ITINERARY_DATE);
        }
    }

    private String buildPrompt(Itinerary itinerary, UserPreferences preferences) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là AI Travel Planner chuyên nghiệp của BookingNext.\n");
        prompt.append("Nhiệm vụ: Tạo lịch trình chi tiết theo từng NGÀY và TIME SLOTS.\n\n");

        prompt.append("THÔNG TIN CHUYẾN ĐI:\n");
        prompt.append("- Điểm đến: ").append(itinerary.getDestination() != null ? itinerary.getDestination() : "Chưa xác định").append("\n");
        prompt.append("- Ngày bắt đầu: ").append(itinerary.getStartDate() != null ? itinerary.getStartDate().toString() : "Chưa xác định").append("\n");
        prompt.append("- Ngày kết thúc: ").append(itinerary.getEndDate() != null ? itinerary.getEndDate().toString() : "Chưa xác định").append("\n");
        prompt.append("- Ngân sách dự kiến: ").append(itinerary.getBudget() != null ? itinerary.getBudget().toString() : "Không giới hạn").append(" VND\n");

        if (itinerary.getNotes() != null && !itinerary.getNotes().isEmpty()) {
            prompt.append("\nNGỮ CẢNH BỔ SUNG (tham khảo, không bắt buộc):\n");
            prompt.append("- Ghi chú khách hàng: ").append(itinerary.getNotes()).append("\n");
            prompt.append("→ Sử dụng thông tin này để điều chỉnh lịch trình phù hợp hơn nếu có thể.\n");
            prompt.append("→ Ví dụ: 'ít đá' vẫn có đồ uống nhưng giảm đá, 'ưu tiên hải sản' đề xuất nhà hàng hải sản.\n");
        }

        prompt.append("\nSỞ THÍCH NGƯỜI DÙNG:\n");
        if (preferences != null) {
            prompt.append("- Phong cách du lịch: ").append(preferences.getTravelStyle()).append("\n");
            prompt.append("- Mức ngân sách: ").append(preferences.getBudgetLevel()).append("\n");
            prompt.append("- Đi cùng: ").append(preferences.getTravelingWith()).append("\n");
            prompt.append("- Có trẻ em: ").append(preferences.getHasChildren() != null && preferences.getHasChildren() ? "Có" : "Không").append("\n");

            if (preferences.getInterests() != null && !preferences.getInterests().isEmpty()) {
                prompt.append("- Sở thích: ").append(String.join(", ", preferences.getInterests())).append("\n");
            }
            if (preferences.getPreferredActivities() != null && !preferences.getPreferredActivities().isEmpty()) {
                prompt.append("- Hoạt động ưa thích: ").append(String.join(", ", preferences.getPreferredActivities())).append("\n");
            }
            if (preferences.getDietaryRestrictions() != null && !preferences.getDietaryRestrictions().isEmpty()) {
                prompt.append("- Chế độ ăn: ").append(preferences.getDietaryRestrictions()).append("\n");
            }
        } else {
            prompt.append("- Phong cách du lịch: ").append(PredefinedTravelStyle.DEFAULT_TRAVEL_STYLE).append("\n");
            prompt.append("- Mức ngân sách: ").append(PredefinedBudgetLevel.DEFAULT_BUDGET_LEVEL).append("\n");
            prompt.append("- Đi cùng: ").append(PredefinedTravelingWith.DEFAULT_TRAVELING_WITH).append("\n");
            prompt.append("- Có trẻ em: Không\n");
        }

        prompt.append("\nQUY TẮC TẠO LỊCH TRÌNH:\n");
        prompt.append("1. Phân chia theo NGÀY (day 1, day 2, ...)\n");
        prompt.append("2. Mỗi ngày có các TIME SLOTS cụ thể với giờ bắt đầu và kết thúc (VD: 08:00 - 09:00)\n");
        prompt.append("3. Mỗi slot gồm: time (HH:mm), end_time (HH:mm), type, name, location, estimated_price (VND), duration_minutes\n");
        prompt.append("4. Các loại activity type hợp lệ: ").append(String.join(", ", VALID_ITEM_TYPES)).append("\n");
        prompt.append("5. Ưu tiên sở thích khách hàng\n");
        prompt.append("6. Đảm bảo logical flow (di chuyển hợp lý, không quá dồn)\n");
        prompt.append("7. Bao gồm: breakfast, lunch, dinner cho mỗi ngày\n");
        prompt.append("8. ref_id để null vì AI chưa biết ID cụ thể từ database\n");
        prompt.append("9. display_order bắt đầu từ 1, tăng dần theo thứ tự thời gian\n\n");

        prompt.append("IMPORTANT: Return ONLY valid JSON. Do not include any explanation, markdown, or text outside the JSON. The JSON must be complete with all closing braces. Do not truncate.\n\n");

        prompt.append("OUTPUT FORMAT (JSON only):\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"Tổng quan chuyến đi\",\n");
        prompt.append("  \"total_estimated_days\": SỐ_NGÀY,\n");
        prompt.append("  \"days\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"day\": 1,\n");
        prompt.append("      \"date\": \"YYYY-MM-DD\",\n");
        prompt.append("      \"theme\": \"Chủ đề ngày\",\n");
        prompt.append("      \"slots\": [\n");
        prompt.append("        {\n");
        prompt.append("          \"time\": \"HH:MM\",\n");
        prompt.append("          \"end_time\": \"HH:MM\",\n");
        prompt.append("          \"type\": \"activity_type\",\n");
        prompt.append("          \"name\": \"Tên hoạt động\",\n");
        prompt.append("          \"location\": \"Địa điểm\",\n");
        prompt.append("          \"estimated_price\": SỐ_TIỀN,\n");
        prompt.append("          \"duration_minutes\": SỐ_PHÚT,\n");
        prompt.append("          \"ref_id\": null,\n");
        prompt.append("          \"display_order\": SỐ_THỨ_TỰ\n");
        prompt.append("        }\n");
        prompt.append("      ]\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    private List<ItineraryItem> parseAndSaveItems(String jsonResponse, Itinerary itinerary) throws Exception {
        List<ItineraryItem> allItems = new ArrayList<>();

        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        JsonNode daysNode = rootNode.get("days");
        if (daysNode == null || !daysNode.isArray()) {
            log.error("JSON response không có 'days' array hoặc 'days' không phải array");
            throw new ApiException(ErrorCode.ITINERARY_CANNOT_GENERATE);
        }

        int displayOrder = 1;
        for (JsonNode dayNode : daysNode) {
            int day = dayNode.get("day").asInt();

            JsonNode dateNode = dayNode.get("date");
            LocalDate date = null;
            if (dateNode != null && !dateNode.isNull()) {
                try {
                    date = LocalDate.parse(dateNode.asText(), DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    log.warn("Cannot parse date: {}", dateNode.asText());
                }
            }

            JsonNode slotsNode = dayNode.get("slots");
            if (slotsNode != null && slotsNode.isArray()) {
                for (JsonNode slotNode : slotsNode) {
                    String itemTypeRaw = getTextOrNull(slotNode, "type");
                    String validatedItemType = validateItemType(itemTypeRaw);

                    ItineraryItem item = ItineraryItem.builder()
                            .itinerary(itinerary)
                            .itemType(validatedItemType)
                            .scheduledDate(date)
                            .startTime(parseTime(getTextOrNull(slotNode, "time")))
                            .endTime(parseTime(getTextOrNull(slotNode, "end_time")))
                            .suggestedName(getTextOrNull(slotNode, "name"))
                            .suggestedLocation(getTextOrNull(slotNode, "location"))
                            .suggestedPrice(parseBigDecimal(slotNode, "estimated_price"))
                            .estimatedDurationMinutes(parseInteger(slotNode, "duration_minutes"))
                            .refId(null)
                            .bookingStatus(PredefinedStatusItineraryItem.PENDING)
                            .paymentStatus(PredefinedPaymentStatus.UNPAID)
                            .displayOrder(displayOrder++)
                            .build();

                    allItems.add(item);
                    itineraryService.saveItineraryItem(item);
                }
            }
        }

        return allItems;
    }

    private String validateItemType(String type) {
        if (type == null || type.isEmpty()) {
            log.warn("Item type is null or empty, defaulting to: {}", PredefinedItemType.ACTIVITY);
            return PredefinedItemType.ACTIVITY;
        }

        String normalizedType = type.toLowerCase().trim();

        String mappedType = ITEM_TYPE_ALIASES.get(normalizedType);
        if (mappedType != null) {
            log.info("Mapped item type '{}' to '{}'", type, mappedType);
            return mappedType;
        }

        if (!VALID_ITEM_TYPES.contains(normalizedType)) {
            log.warn("Invalid item type: '{}', defaulting to: {}", type, PredefinedItemType.ACTIVITY);
            return PredefinedItemType.ACTIVITY;
        }

        return normalizedType;
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText() : null;
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(timeStr, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("Cannot parse time: {}", timeStr);
            return null;
        }
    }

    private BigDecimal parseBigDecimal(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(fieldNode.asText());
        } catch (NumberFormatException e) {
            log.warn("Cannot parse BigDecimal: {}", fieldNode.asText());
            return null;
        }
    }

    private Integer parseInteger(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        return fieldNode.asInt();
    }
}
