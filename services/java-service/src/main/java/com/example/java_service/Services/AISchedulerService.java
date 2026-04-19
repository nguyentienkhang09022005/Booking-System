package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedBudgetLevel;
import com.example.java_service.Constant.PredefinedItemType;
import com.example.java_service.Constant.PredefinedStatusItinerary;
import com.example.java_service.Constant.PredefinedTravelStyle;
import com.example.java_service.Constant.PredefinedTravelingWith;
import com.example.java_service.Dto.Response.*;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Helpers.SchedulingTools;
import com.example.java_service.Models.Itinerary;
import com.example.java_service.Models.ItineraryItem;
import com.example.java_service.Models.UserPreferences;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@Slf4j
public class AISchedulerService {

    private final ChatLanguageModel chatModel;
    private final SchedulingTools schedulingTools;
    private final UserPreferencesService userPreferencesService;
    private final ItineraryService itineraryService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final Set<String> VALID_ITEM_TYPES = new HashSet<>(Arrays.asList(
            PredefinedItemType.FLIGHT,
            PredefinedItemType.ACCOMMODATION,
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
        ITEM_TYPE_ALIASES.put("hotel", PredefinedItemType.ACCOMMODATION);
        ITEM_TYPE_ALIASES.put("homestay", PredefinedItemType.ACCOMMODATION);
        ITEM_TYPE_ALIASES.put("resort", PredefinedItemType.ACCOMMODATION);
        ITEM_TYPE_ALIASES.put("restaurant", PredefinedItemType.MEAL);
        ITEM_TYPE_ALIASES.put("food", PredefinedItemType.MEAL);
        ITEM_TYPE_ALIASES.put("sightseeing", PredefinedItemType.ACTIVITY);
        ITEM_TYPE_ALIASES.put("visit", PredefinedItemType.ACTIVITY);
        ITEM_TYPE_ALIASES.put("tour", PredefinedItemType.TOUR);
    }

    private static final Map<String, String> TICKET_TYPE_KEYWORDS = new HashMap<>();
    static {
        TICKET_TYPE_KEYWORDS.put("máy bay", "Plane");
        TICKET_TYPE_KEYWORDS.put("plane", "Plane");
        TICKET_TYPE_KEYWORDS.put("airplane", "Plane");
        TICKET_TYPE_KEYWORDS.put("xe khách", "Coach");
        TICKET_TYPE_KEYWORDS.put("coach", "Coach");
        TICKET_TYPE_KEYWORDS.put("bus", "Coach");
        TICKET_TYPE_KEYWORDS.put("tàu hỏa", "Train");
        TICKET_TYPE_KEYWORDS.put("train", "Train");
        TICKET_TYPE_KEYWORDS.put("tàu cao tốc", "PassengerShip");
        TICKET_TYPE_KEYWORDS.put("tàu thuyền", "PassengerShip");
        TICKET_TYPE_KEYWORDS.put("passengership", "PassengerShip");
    }

    public AISchedulerService(
            ChatLanguageModel chatModel,
            SchedulingTools schedulingTools,
            UserPreferencesService userPreferencesService,
            ItineraryService itineraryService,
            ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.schedulingTools = schedulingTools;
        this.userPreferencesService = userPreferencesService;
        this.itineraryService = itineraryService;
        this.objectMapper = objectMapper;
    }

    public synchronized ItineraryResponse generateSchedule(String idItinerary) {
        Itinerary itinerary = itineraryService.getItineraryEntity(idItinerary);

        validateItineraryForGeneration(itinerary);

        UserPreferences preferences = userPreferencesService.getPreferencesEntityOrDefault(
                itinerary.getUsers().getIdUser().toString());

        String requirements = buildRequirements(itinerary, preferences);

        // ========== STEP 1: LLM Call #1 - Generate Schedule with Needs ==========
        String generatedScheduleJson = generateScheduleWithNeeds(requirements, itinerary);
        log.info("Generated schedule: {}", generatedScheduleJson);

        // ========== STEP 2: Parse Needs and Fetch Data from DB ==========
        Map<String, Object> parsedNeeds = parseNeeds(generatedScheduleJson);
        Map<String, String> dbData = fetchDataFromDB(parsedNeeds, itinerary);

        // ========== STEP 3: LLM Call #2 - Enrichment ==========
        String enrichedScheduleJson = enrichScheduleWithDBData(generatedScheduleJson, dbData, requirements);
        log.info("Enriched schedule: {}", enrichedScheduleJson);

        // ========== STEP 4: Parse and Save Items ==========
        try {
            itineraryService.deleteItineraryItemsByItineraryId(itinerary.getIdItinerary());

            List<ItineraryItem> items = parseAndSaveItems(enrichedScheduleJson, itinerary, preferences);

            BigDecimal totalCost = items.stream()
                    .map(ItineraryItem::getSuggestedPrice)
                    .filter(price -> price != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            itinerary.setTotalEstimatedCost(totalCost);
            itineraryService.updateItineraryStatus(idItinerary, PredefinedStatusItinerary.AI_GENERATED);

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi parse và lưu lịch trình: {}", e.getMessage());
            throw new ApiException(ErrorCode.ITINERARY_CANNOT_GENERATE);
        }

        ItineraryResponse response = itineraryService.getItinerary(idItinerary);

        // ========== STEP 5: Fetch Tour Suggestions ==========
        List<TourForScheduleResponse> suggestedTours = fetchSuggestedTours(itinerary.getDestination(), itinerary.getBudget());
        response.setSuggestedTours(suggestedTours);

        return response;
    }

    // ========== STEP 1: Generate Schedule with Needs ==========
    private String generateScheduleWithNeeds(String requirements, Itinerary itinerary) {
        String systemPrompt = buildSystemPromptForGeneration();

        Map<String, Object> context = new HashMap<>();
        context.put("destination", itinerary.getDestination());
        context.put("startDate", itinerary.getStartDate() != null ? itinerary.getStartDate().toString() : "");
        context.put("endDate", itinerary.getEndDate() != null ? itinerary.getEndDate().toString() : "");

        String userMessage = buildUserMessageForGeneration(requirements, context);

        String jsonResponse = callLlmWithRetry(systemPrompt, userMessage);

        return extractJson(jsonResponse);
    }

    private String buildSystemPromptForGeneration() {
        return """
                Bạn là AI Travel Planner chuyên nghiệp của BookingNext.

                === NHIỆM VỤ ===
                Tạo lịch trình du lịch chi tiết theo từng NGÀY và TIME SLOTS.

                === NGUYÊN TẮC NGHIÊM NGẶT - ĐỌC KỸ ===
                1. Chỉ trả về JSON hợp lệ, bắt đầu bằng '{' và kết thúc bằng '}'
                2. ref_id: CHỈ sử dụng UUID thực từ database HOẶC để NULL tuyệt đối!
                   - KHÔNG BAO GIỜ tự tạo UUID giả như: 'uuid-xxx', 'xxx-xxx-xxx', 'id123', 'abc123'
                   - Nếu không chắc chắn → để ref_id = null
                3. GIÁ estimate dựa trên market, không điều chỉnh tùy ý
                4. Để tổng budget hợp lý, không quá ngân sách
                5. Mỗi slot cần có 'note' mô tả ngắn gọi (có thể trống)

                === VÍ DỤ UUID HỢP LỆ ===
                - 550e8400-e29b-41d4-a716-446655440000 ✓
                - a3f8c9b2-1e4d-4f7a-8c9b-2d3e4f5a6b7c ✗ (fake)

                === OUTPUT FORMAT ===
                {
                  "summary": "Tổng quan lịch trình",
                  "total_estimated_days": N,
                  "needs": {
                    "accommodations": true/false,
                    "tours": true/false,
                    "transport_from": "điểm đi (nếu cần transport)",
                    "transport_to": "điểm đến (nếu cần transport)",
                    "restaurants": true/false,
                    "activities": true/false
                  },
                  "days": [
                    {
                      "day": 1,
                      "date": "YYYY-MM-DD",
                      "theme": "Chủ đề ngày",
                      "slots": [
                        {
                          "time": "HH:MM",
                          "end_time": "HH:MM",
                          "type": "ACCOMMODATION|TRANSPORT|MEAL|ACTIVITY|TOUR|FLIGHT",
                          "name": "Tên item",
                          "location": "Địa điểm cụ thể",
                          "estimated_price": N,
                          "duration_minutes": N,
                          "ref_id": "UUID_THỰC_TỪ_DB_HOẶC_NULL",
                          "note": "Ghi chú bổ sung nếu có"
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    private String buildUserMessageForGeneration(String requirements, Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hãy tạo lịch trình du lịch chi tiết:\n\n");
        sb.append(requirements);
        sb.append("\n\nĐiểm đến: ").append(context.get("destination"));
        sb.append("\nNgày bắt đầu: ").append(context.get("startDate"));
        sb.append("\nNgày kết thúc: ").append(context.get("endDate"));
        sb.append("\n\nYÊU CẦU:\n");
        sb.append("- Phân chia theo từng ngày và time slots\n");
        sb.append("- Xác định rõ 'needs' để biết cần tìm gì từ database\n");
        sb.append("- Với transport, ghi rõ điểm đi và điểm đến trong 'needs'\n");
        sb.append("- Trả về JSON theo format quy định");
        return sb.toString();
    }

    // ========== STEP 2: Parse Needs ==========
    private Map<String, Object> parseNeeds(String generatedJson) {
        Map<String, Object> needs = new HashMap<>();
        try {
            JsonNode rootNode = objectMapper.readTree(generatedJson);
            JsonNode needsNode = rootNode.get("needs");
            if (needsNode != null) {
                needs.put("accommodations", needsNode.has("accommodations") && needsNode.get("accommodations").asBoolean());
                needs.put("tours", needsNode.has("tours") && needsNode.get("tours").asBoolean());
                needs.put("transport_from", needsNode.has("transport_from") ? needsNode.get("transport_from").asText() : null);
                needs.put("transport_to", needsNode.has("transport_to") ? needsNode.get("transport_to").asText() : null);
                needs.put("restaurants", needsNode.has("restaurants") && needsNode.get("restaurants").asBoolean());
                needs.put("activities", needsNode.has("activities") && needsNode.get("activities").asBoolean());
            }
        } catch (Exception e) {
            log.warn("Cannot parse needs from generated JSON: {}", e.getMessage());
        }
        return needs;
    }

    // ========== STEP 2: Fetch Data from DB ==========
    private Map<String, String> fetchDataFromDB(Map<String, Object> needs, Itinerary itinerary) {
        Map<String, String> data = new HashMap<>();
        String destination = itinerary.getDestination();
        BigDecimal budget = itinerary.getBudget();

        try {
            // Fetch Accommodations
            if (Boolean.TRUE.equals(needs.get("accommodations"))) {
                String accResult = schedulingTools.findAccommodationFullInfo(destination, destination);
                if (accResult != null && !accResult.isEmpty()) {
                    data.put("accommodations", accResult);
                    log.info("Fetched accommodations for: {}", destination);
                }
            }

            // Fetch Tours
            if (Boolean.TRUE.equals(needs.get("tours"))) {
                String tourResult = schedulingTools.findToursByDestinationAndBudget(destination, budget);
                if (tourResult != null && !tourResult.isEmpty()) {
                    data.put("tours", tourResult);
                    log.info("Fetched tours for: {}", destination);
                }
            }

            // Fetch Transport
            String transportFrom = (String) needs.get("transport_from");
            String transportTo = (String) needs.get("transport_to");
            if (transportFrom != null && transportTo != null && !transportFrom.isEmpty() && !transportTo.isEmpty()) {
                String transportResult = schedulingTools.findTransportByRoute(transportFrom, transportTo, null, budget);
                if (transportResult != null && !transportResult.isEmpty()) {
                    data.put("transport", transportResult);
                    log.info("Fetched transport from {} to {}", transportFrom, transportTo);
                }
            }

            // Fetch Restaurants (as services with type restaurant)
            if (Boolean.TRUE.equals(needs.get("restaurants"))) {
                String restResult = schedulingTools.searchRestaurantIds(destination);
                if (restResult != null && !restResult.isEmpty() && !restResult.equals("[]")) {
                    data.put("restaurants", restResult);
                    log.info("Fetched restaurants for: {}", destination);
                }
            }

            // Fetch Activities (as services with type activity)
            if (Boolean.TRUE.equals(needs.get("activities"))) {
                String actResult = schedulingTools.searchActivityIds(destination);
                if (actResult != null && !actResult.isEmpty() && !actResult.equals("[]")) {
                    data.put("activities", actResult);
                    log.info("Fetched activities for: {}", destination);
                }
            }

        } catch (Exception e) {
            log.error("Error fetching data from DB: {}", e.getMessage());
        }

        return data;
    }

    // ========== STEP 3: Enrichment ==========
    private String enrichScheduleWithDBData(String generatedJson, Map<String, String> dbData, String requirements) {
        String systemPrompt = buildSystemPromptForEnrichment();
        
        StringBuilder userMessage = new StringBuilder();
        userMessage.append("=== LỊCH TRÌNH AI ĐÃ GENERATE ===\n");
        userMessage.append(generatedJson);
        userMessage.append("\n\n=== DỮ LIỆU TỪ DATABASE ===\n");
        
        for (Map.Entry<String, String> entry : dbData.entrySet()) {
            userMessage.append("[").append(entry.getKey().toUpperCase()).append("]\n");
            userMessage.append(entry.getValue()).append("\n\n");
        }
        
        userMessage.append("=== YÊU CẦU ===\n");
        userMessage.append("Dựa trên dữ liệu thực từ database:\n");
        userMessage.append("1. Cập nhật GIÁ chính xác từ database cho các item có trong DB\n");
        userMessage.append("2. Gắn ref_id cho các item có trong database (UUID thực)\n");
        userMessage.append("3. Giữ nguyên các item không có trong database (ref_id = null, giữ nguyên price AI estimate)\n");
        userMessage.append("4. CHỈ sử dụng UUID có trong dữ liệu, không bịa đặt\n");
        userMessage.append("5. ĐIỀU CHỈNH duration CHO PHÙ HỢP VỚI THỰC TẾ:\n");
        userMessage.append("   - ACCOMMODATION: 1 đêm = 24 tiếng (checkin 12h trưa → checkout 12h trưa mai)\n");
        userMessage.append("   - TRANSPORT: Lấy từ DB (departure_time - arrival_time). Nếu DB không có, estimate lại:\n");
        userMessage.append("     * Máy bay nội địa: 1-2 tiếng\n");
        userMessage.append("     * Xe khách: ~45 phút cho mỗi 50km\n");
        userMessage.append("     * Tàu hỏa: ~40 phút cho mỗi 50km\n");
        userMessage.append("   - MEAL: 45-90 phút\n");
        userMessage.append("   - ACTIVITY: Theo thực tế (tham quan museum 1-2 tiếng, tour city 4-6 tiếng, spa 1-2 tiếng)\n");
        userMessage.append("   - Khi duration thay đổi → điều chỉnh giá theo tỷ lệ!\n\n");
        userMessage.append("Với TRANSPORT: Parse name để xác định ticket_type và tìm transport phù hợp trong DB.\n");
        userMessage.append("Với ACCOMMODATION: Tìm accommodation trong DB. Nếu duration < 12 tiếng → đổi thành 24 tiếng.\n");
        userMessage.append("Với TOUR: Nếu AI đề xuất tour cụ thể → tìm trong DB và gắn ref_id.\n\n");
        userMessage.append("Trả về JSON đã được cập nhật với ref_id, estimated_price và duration_minutes chính xác.\n");
        userMessage.append("Giữ nguyên cấu trúc JSON.\n");

        String jsonResponse = callLlmWithRetry(systemPrompt, userMessage.toString());
        return extractJson(jsonResponse);
    }

    private String buildSystemPromptForEnrichment() {
        return """
                Bạn là AI Travel Planner chuyên nghiệp của BookingNext.

                === NHIỆM VỤ ===
                Cập nhật lịch trình du lịch với dữ liệu từ database thực tế.

                === NGUYÊN TẮC NGHIÊM NGẶT ===
                1. Chỉ trả về JSON hợp lệ, bắt đầu bằng '{' và kết thúc bằng '}'
                2. ref_id PHẢI là UUID thực từ database HOẶC null nếu không có
                3. Giữ nguyên các item không có trong database (ref_id = null)
                4. CHỈ sử dụng UUID có trong dữ liệu database đã cung cấp
                5. KHÔNG bịa đặt UUID
                6. ĐIỀU CHỈNH duration CHO PHÙ HỢP VỚI THỰC TẾ
                7. KHI CÓ ref_id → LẤY GIÁ TỪ DATABASE, KHÔNG DÙNG GIÁ AI ESTIMATE

                === DURATION GUIDELINES ===
                - ACCOMMODATION: 1 đêm tiêu chuẩn = 24 tiếng (checkin 12h trưa → checkout 12h trưa mai)
                  * Nếu AI estimate < 12 tiếng → đổi thành 24 tiếng (1 đêm)
                  * Nếu >= 12 tiếng → giữ nguyên (có thể là multi-night)
                  
                - TRANSPORT: Lấy từ database (departure_time - arrival_time)
                  * Nếu DB không có → estimate lại realistic:
                    - Máy bay nội địa: 1-2 tiếng
                    - Xe khách: ~45 phút cho mỗi 50km (VD: 300km = ~4.5 tiếng)
                    - Tàu hỏa: ~40 phút cho mỗi 50km
                    
                - MEAL: 45-90 phút (breakfast/lunch/dinner)
                
                - ACTIVITY: Theo thực tế:
                  * Tham quan museum/di tích: 1-2 tiếng
                  * Tour city trọn ngày: 4-6 tiếng
                  * Spa/massage: 1-2 tiếng
                  * Beach activities: 3-5 tiếng

                === GIÁ (QUAN TRỌNG) ===
                ** KHI CÓ ref_id → BẮT BUỘC LẤY GIÁ TỪ DATABASE **
                - ACCOMMODATION: Lấy giá từ rooms trong DB data
                - TRANSPORT: Lấy giá từ transport ticket trong DB data
                - TOUR: Lấy adultPrice từ tour trong DB data
                
                ** KHI KHÔNG CÓ ref_id → GIỮ NGUYÊN GIÁ AI ESTIMATE **

                === VỚI TRANSPORT ===
                - Parse name để xác định ticket_type:
                  * "máy bay" → Plane
                  * "xe khách" → Coach
                  * "tàu hỏa" → Train
                  * "tàu cao tốc/tàu thuyền" → PassengerShip
                - Tìm transport trong DB theo route và ticket_type
                - Lấy duration từ DB nếu có
                - Lấy GIÁ từ DB transport

                === VỚI ACCOMMODATION ===
                - Tìm accommodation trong DB phù hợp với name AI generate
                - Nếu tìm được → gắn ref_id và LẤY GIÁ TỪ DB (rooms)
                - Điều chỉnh duration: nếu < 12 tiếng → đổi thành 24 tiếng
                - Nếu duration thay đổi → điều chỉnh giá theo tỷ lệ với DB price

                === VỚI TOUR ===
                - Nếu AI đề xuất tour cụ thể → tìm trong DB và gắn ref_id
                - Lấy adultPrice từ DB
                - Nếu không có trong DB → ref_id = null

                Trả về JSON đã được cập nhật với ref_id chính xác và GIÁ TỪ DATABASE.
                """;
    }

    // ========== Parse and Save Items ==========
    private List<ItineraryItem> parseAndSaveItems(String jsonResponse, Itinerary itinerary, UserPreferences preferences) throws Exception {
        List<ItineraryItem> allItems = new ArrayList<>();

        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        JsonNode daysNode = rootNode.get("days");
        if (daysNode == null || !daysNode.isArray()) {
            log.error("JSON response không có 'days' array hoặc 'days' không phải array");
            throw new ApiException(ErrorCode.ITINERARY_CANNOT_GENERATE);
        }

        int displayOrder = 1;
        for (JsonNode dayNode : daysNode) {
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
                    UUID refId = parseRefId(slotNode, "ref_id");

                    BigDecimal price = parseBigDecimal(slotNode, "estimated_price");
                    Integer duration = parseInteger(slotNode, "duration_minutes");

                    // Adjust duration và price dựa trên item type
                    ItineraryItem adjustedItem = adjustDurationAndPrice(
                            validatedItemType, refId, price, duration);
                    duration = adjustedItem.getEstimatedDurationMinutes();
                    price = adjustedItem.getSuggestedPrice();

                    ItineraryItem item = ItineraryItem.builder()
                            .itinerary(itinerary)
                            .itemType(validatedItemType)
                            .scheduledDate(date)
                            .startTime(parseTime(getTextOrNull(slotNode, "time")))
                            .endTime(parseTime(getTextOrNull(slotNode, "end_time")))
                            .suggestedName(getTextOrNull(slotNode, "name"))
                            .suggestedLocation(getTextOrNull(slotNode, "location"))
                            .suggestedPrice(price)
                            .estimatedDurationMinutes(duration)
                            .refId(refId)
                            .displayOrder(displayOrder++)
                            .build();

                    allItems.add(item);
                    itineraryService.saveItineraryItem(item);
                }
            }
        }

        return allItems;
    }

    // ========== Adjust Duration and Price ==========
    private ItineraryItem adjustDurationAndPrice(String itemType, UUID refId, BigDecimal originalPrice, Integer originalDuration) {
        BigDecimal adjustedPrice = originalPrice;
        Integer adjustedDuration = originalDuration;

        if (itemType == null) {
            return ItineraryItem.builder()
                    .suggestedPrice(originalPrice)
                    .estimatedDurationMinutes(originalDuration)
                    .build();
        }

        switch (itemType) {
            case PredefinedItemType.ACCOMMODATION:
                if (adjustedDuration == null || adjustedDuration < 1440) { // < 24 tiếng
                    adjustedDuration = 1440; // 24 tiếng = 1 đêm
                    if (originalPrice != null && originalDuration != null && originalDuration > 0) {
                        // Điều chỉnh giá theo tỷ lệ: new_price = original_price * (new_duration / original_duration)
                        // Nhưng vì accommodation thường estimate sai, ta giả định originalPrice là price/night
                        // Nên nếu AI estimate < 12 tiếng, có thể họ đang estimate cho 1 nửa đêm
                        // → giá vẫn giữ nguyên (vì DB price là price/night)
                        adjustedPrice = originalPrice;
                    }
                    log.info("Adjusted ACCOMMODATION duration from {} to 1440 minutes (24h)", originalDuration);
                }
                break;

            case PredefinedItemType.TRANSPORT:
                if (refId != null && adjustedDuration == null) {
                    // Fetch duration từ DB
                    try {
                        String transportJson = schedulingTools.getTransportDetails(refId.toString());
                        if (transportJson != null && !transportJson.equals("{}")) {
                            TransportForScheduleResponse transport = objectMapper.readValue(transportJson, TransportForScheduleResponse.class);
                            if (transport.getDurationMinutes() != null) {
                                adjustedDuration = transport.getDurationMinutes();
                                log.info("Adjusted TRANSPORT duration from DB: {} minutes", adjustedDuration);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Cannot fetch transport duration from DB: {}", e.getMessage());
                    }
                }
                break;

            case PredefinedItemType.MEAL:
                if (adjustedDuration == null || adjustedDuration < 30) {
                    adjustedDuration = 60; // Mặc định 1 tiếng cho meal
                    log.info("Adjusted MEAL duration from {} to 60 minutes", originalDuration);
                }
                break;

            case PredefinedItemType.ACTIVITY:
                if (adjustedDuration == null) {
                    adjustedDuration = 120; // Mặc định 2 tiếng cho activity
                    log.info("Set default ACTIVITY duration to 120 minutes");
                }
                break;

            default:
                // FLIGHT, TOUR giữ nguyên
                break;
        }

        // Nếu duration thay đổi và có refId, điều chỉnh price theo tỷ lệ
        if (refId != null && originalDuration != null && originalDuration > 0 
                && adjustedDuration != null && !adjustedDuration.equals(originalDuration)) {
            BigDecimal ratio = BigDecimal.valueOf(adjustedDuration)
                    .divide(BigDecimal.valueOf(originalDuration), 2, java.math.RoundingMode.HALF_UP);
            adjustedPrice = originalPrice.multiply(ratio);
            log.info("Adjusted price from {} to {} based on duration ratio {}", 
                    originalPrice, adjustedPrice, ratio);
        }

        return ItineraryItem.builder()
                .suggestedPrice(adjustedPrice)
                .estimatedDurationMinutes(adjustedDuration)
                .build();
    }

    // ========== Tour Suggestions ==========
    private List<TourForScheduleResponse> fetchSuggestedTours(String destination, BigDecimal budget) {
        try {
            String tourIdsJson = schedulingTools.findToursByDestinationAndBudget(destination, budget != null ? budget : BigDecimal.valueOf(Long.MAX_VALUE));
            if (tourIdsJson == null || tourIdsJson.isEmpty() || tourIdsJson.equals("[]")) {
                return List.of();
            }
            
            List<String> tourIds = objectMapper.readValue(tourIdsJson, List.class);
            List<TourForScheduleResponse> tours = new ArrayList<>();
            
            for (String id : tourIds) {
                if (isValidUuid(id)) {
                    String tourDetails = schedulingTools.getTourDetails(id);
                    if (tourDetails != null && !tourDetails.isEmpty() && !tourDetails.equals("{}")) {
                        TourForScheduleResponse tour = objectMapper.readValue(tourDetails, TourForScheduleResponse.class);
                        tours.add(tour);
                    }
                }
            }
            
            log.info("Found {} suggested tours for destination: {}", tours.size(), destination);
            return tours;
        } catch (Exception e) {
            log.error("Error fetching suggested tours: {}", e.getMessage());
            return List.of();
        }
    }

    // ========== Utility Methods ==========
    private String callLlmWithRetry(String systemPrompt, String userMessage) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                ChatRequest request = ChatRequest.builder()
                        .messages(List.of(
                                SystemMessage.from(systemPrompt),
                                UserMessage.from(userMessage)
                        ))
                        .build();

                String response = chatModel.chat(request).aiMessage().text();

                if (response != null && !response.isEmpty()) {
                    log.info("LLM call attempt {} successful", attempt + 1);
                    return response;
                }

                log.warn("LLM call attempt {} returned empty response", attempt + 1);
            } catch (Exception e) {
                log.error("LLM call attempt {} error: {}", attempt + 1, e.getMessage());
                if (attempt == 2) {
                    throw new ApiException(ErrorCode.ITINERARY_CANNOT_GENERATE);
                }
            }
        }
        throw new ApiException(ErrorCode.ITINERARY_CANNOT_GENERATE);
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

    private String buildRequirements(Itinerary itinerary, UserPreferences preferences) {
        StringBuilder sb = new StringBuilder();

        sb.append("THÔNG TIN CHUYẾN ĐI:\n");
        sb.append("- Điểm đến: ").append(itinerary.getDestination() != null ? itinerary.getDestination() : "Chưa xác định").append("\n");
        sb.append("- Ngày bắt đầu: ").append(itinerary.getStartDate() != null ? itinerary.getStartDate().toString() : "Chưa xác định").append("\n");
        sb.append("- Ngày kết thúc: ").append(itinerary.getEndDate() != null ? itinerary.getEndDate().toString() : "Chưa xác định").append("\n");
        sb.append("- Ngân sách dự kiến: ").append(itinerary.getBudget() != null ? itinerary.getBudget().toString() : "Không giới hạn").append(" VND\n");

        if (itinerary.getNotes() != null && !itinerary.getNotes().isEmpty()) {
            sb.append("\nNGỮ CẢNH BỔ SUNG:\n");
            sb.append("- Ghi chú khách hàng: ").append(itinerary.getNotes()).append("\n");
        }

        sb.append("\nSỞ THÍCH NGƯỜI DÙNG:\n");
        if (preferences != null) {
            sb.append("- Phong cách du lịch: ").append(nullSafe(preferences.getTravelStyle())).append("\n");
            sb.append("- Mức ngân sách: ").append(nullSafe(preferences.getBudgetLevel())).append("\n");
            sb.append("- Đi cùng: ").append(nullSafe(preferences.getTravelingWith())).append("\n");
            sb.append("- Có trẻ em: ").append(preferences.getHasChildren() != null && preferences.getHasChildren() ? "Có" : "Không").append("\n");

            if (preferences.getInterests() != null && !preferences.getInterests().isEmpty()) {
                sb.append("- Sở thích: ").append(String.join(", ", preferences.getInterests())).append("\n");
            }
            if (preferences.getPreferredActivities() != null && !preferences.getPreferredActivities().isEmpty()) {
                sb.append("- Hoạt động ưa thích: ").append(String.join(", ", preferences.getPreferredActivities())).append("\n");
            }
            if (preferences.getDietaryRestrictions() != null && !preferences.getDietaryRestrictions().isEmpty()) {
                sb.append("- Chế độ ăn: ").append(preferences.getDietaryRestrictions()).append("\n");
            }
        } else {
            sb.append("- Phong cách du lịch: ").append(PredefinedTravelStyle.DEFAULT_TRAVEL_STYLE).append("\n");
            sb.append("- Mức ngân sách: ").append(PredefinedBudgetLevel.DEFAULT_BUDGET_LEVEL).append("\n");
            sb.append("- Đi cùng: ").append(PredefinedTravelingWith.DEFAULT_TRAVELING_WITH).append("\n");
            sb.append("- Có trẻ em: Không\n");
        }

        return sb.toString();
    }

    private String nullSafe(String value) {
        return value != null ? value : "Không xác định";
    }

    private String extractJson(String response) {
        if (response == null || response.isEmpty()) {
            return "{}";
        }

        String cleaned = response.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*$", "");
        cleaned = cleaned.trim();

        if (cleaned.startsWith("{")) {
            int lastBrace = cleaned.lastIndexOf("}");
            if (lastBrace != -1) {
                return cleaned.substring(0, lastBrace + 1);
            }
            return cleaned;
        }

        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            return cleaned.substring(start, end + 1);
        }

        return "{}";
    }

    private UUID parseRefId(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        try {
            String refIdStr = fieldNode.asText();
            if (refIdStr == null || refIdStr.isEmpty() || refIdStr.equalsIgnoreCase("null")) {
                return null;
            }
            if (!isValidUuid(refIdStr)) {
                log.warn("Invalid UUID format: {}", refIdStr);
                return null;
            }
            return UUID.fromString(refIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("Cannot parse ref_id as UUID: {}", fieldNode.asText());
            return null;
        }
    }

    private boolean isValidUuid(String str) {
        if (str == null || str.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
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
