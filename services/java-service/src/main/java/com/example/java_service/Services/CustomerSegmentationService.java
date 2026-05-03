package com.example.java_service.Services;

import com.example.java_service.Dto.Response.CustomerSegmentationResponse;
import com.example.java_service.Events.BookingCompletedEvent;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.UserRepository;
import com.example.java_service.Services.Interfaces.ICustomerSegmentationAgent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class CustomerSegmentationService {

    private final UserRepository userRepository;
    private final ICustomerSegmentationAgent segmentationAgent;
    private final UserAnalyticsService userAnalyticsService;
    private final ObjectMapper objectMapper;

    public CustomerSegmentationService(UserRepository userRepository,
                                       ICustomerSegmentationAgent segmentationAgent,
                                       UserAnalyticsService userAnalyticsService,
                                       ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.segmentationAgent = segmentationAgent;
        this.userAnalyticsService = userAnalyticsService;
        this.objectMapper = objectMapper;
    }

    private static final double CONFIDENCE_THRESHOLD_HIGH = 0.8;
    private static final double CONFIDENCE_THRESHOLD_LOW = 0.5;
    private static final double RECALCULATE_CONFIDENCE_IMPROVEMENT = 0.15;
    private static final int STALE_MONTHS = 6;

    @Async("segmentationExecutor")
    public CompletableFuture<Void> analyzeAndUpdateSegmentAsync(UUID userId) {
        try {
            analyzeAndUpdateSegment(userId);
        } catch (Exception e) {
            log.error("Error analyzing segment for user {}: {}", userId, e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    public void analyzeAndUpdateSegment(UUID userId) {
        Users user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User not found: {}", userId);
            return;
        }

        if (shouldSkipAnalysis(user)) {
            log.debug("Skipping analysis for user {} - confidence is high enough or no behavior change", userId);
            return;
        }

        String userDataJson = userAnalyticsService.getUserAnalyticsData(userId);

        String aiResponse;
        try {
            aiResponse = segmentationAgent.analyzeCustomer(userDataJson);
        } catch (Exception e) {
            log.error("AI segmentation failed for user {}: {}", userId, e.getMessage());
            return;
        }

        try {
            CustomerSegmentationResponse result = parseSegmentationResponse(aiResponse);
            if (result == null || result.getSegment() == null || result.getSegment().isBlank()) {
                log.warn("Invalid segment response for user {}: {}", userId, aiResponse);
                return;
            }

            if (shouldUpdateSegment(user, result.getSegment(), result.getConfidence())) {
                performSegmentUpdate(userId, result.getSegment(), result.getConfidence());
                log.info("Updated segment for user {}: {} (confidence: {})", userId, result.getSegment(), result.getConfidence());
            }
        } catch (Exception e) {
            log.error("Error parsing AI response for user {}: {}", userId, e.getMessage());
        }
    }

    public void performSegmentUpdate(UUID userId, String segment, Double confidence) {
        userRepository.updateCustomerSegment(userId, segment, confidence, LocalDateTime.now());
    }

    private CustomerSegmentationResponse parseSegmentationResponse(String aiResponse) {
        try {
            JsonNode jsonResponse = objectMapper.readTree(aiResponse);
            String segment = jsonResponse.has("segment") ? jsonResponse.get("segment").asText() : null;
            double confidence = jsonResponse.has("confidence") ? jsonResponse.get("confidence").asDouble() : 0.5;
            String reasoning = jsonResponse.has("reasoning") ? jsonResponse.get("reasoning").asText() : null;

            return CustomerSegmentationResponse.builder()
                    .segment(segment)
                    .confidence(confidence)
                    .reasoning(reasoning)
                    .build();
        } catch (Exception e) {
            String jsonStr = extractJsonFromText(aiResponse);
            if (jsonStr != null) {
                try {
                    JsonNode jsonResponse = objectMapper.readTree(jsonStr);
                    String segment = jsonResponse.has("segment") ? jsonResponse.get("segment").asText() : null;
                    double confidence = jsonResponse.has("confidence") ? jsonResponse.get("confidence").asDouble() : 0.5;
                    String reasoning = jsonResponse.has("reasoning") ? jsonResponse.get("reasoning").asText() : null;

                    return CustomerSegmentationResponse.builder()
                            .segment(segment)
                            .confidence(confidence)
                            .reasoning(reasoning)
                            .build();
                } catch (Exception ex) {
                    log.error("Error parsing extracted JSON: {}", ex.getMessage());
                }
            }
            log.error("Error parsing segmentation response: {}", e.getMessage());
            return null;
        }
    }

    private String extractJsonFromText(String text) {
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    private boolean shouldSkipAnalysis(Users user) {
        if (user.getCustomerSegment() == null || user.getSegmentConfidence() == null) {
            return false;
        }

        if (user.getSegmentConfidence() > CONFIDENCE_THRESHOLD_HIGH) {
            if (!isSegmentStale(user)) {
                return true;
            }
        }

        if (user.getSegmentConfidence() > CONFIDENCE_THRESHOLD_LOW && user.getSegmentAnalyzedAt() != null) {
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            if (user.getSegmentAnalyzedAt().isAfter(threeMonthsAgo)) {
                return true;
            }
        }

        return false;
    }

    private boolean isSegmentStale(Users user) {
        if (user.getSegmentAnalyzedAt() == null) {
            return true;
        }
        LocalDateTime staleDate = LocalDateTime.now().minusMonths(STALE_MONTHS);
        return user.getSegmentAnalyzedAt().isBefore(staleDate);
    }

    private boolean shouldUpdateSegment(Users user, String newSegment, double newConfidence) {
        if (user.getCustomerSegment() == null) {
            return true;
        }

        if (!user.getCustomerSegment().equals(newSegment)) {
            return newConfidence > (user.getSegmentConfidence() != null ? user.getSegmentConfidence() : 0)
                    + RECALCULATE_CONFIDENCE_IMPROVEMENT;
        }

        return newConfidence > (user.getSegmentConfidence() != null ? user.getSegmentConfidence() : 0) + RECALCULATE_CONFIDENCE_IMPROVEMENT;
    }

    public String getSegmentForUser(UUID userId) {
        return userRepository.findById(userId)
                .map(Users::getCustomerSegment)
                .orElse(null);
    }

    @Async("segmentationExecutor")
    @EventListener
    public void handleBookingCompletedEvent(BookingCompletedEvent event) {
        log.info("Received BookingCompletedEvent for user: {}", event.getUserId());
        analyzeAndUpdateSegmentAsync(event.getUserId());
    }
}