package com.example.java_service.Controllers;

import com.example.java_service.Dto.Response.*;
import com.example.java_service.Services.BusinessAnalyticsService;
import com.example.java_service.Services.BusinessInsightsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Slf4j
public class BusinessAnalyticsController {

    private final BusinessAnalyticsService analyticsService;
    private final BusinessInsightsService insightsService;

    @GetMapping("/segments")
    public ResponseEntity<SegmentDistributionResponse> getSegments(
            @RequestParam UUID businessId,
            @RequestParam(required = false, defaultValue = "6") Integer months) {
        return ResponseEntity.ok(analyticsService.getSegmentDistribution(businessId, months));
    }

    @GetMapping("/segments/chart")
    public ResponseEntity<SegmentChartResponse> getSegmentsChart(
            @RequestParam UUID businessId,
            @RequestParam(required = false, defaultValue = "6") Integer months) {
        return ResponseEntity.ok(analyticsService.getChartData(businessId, months));
    }

    @GetMapping("/segments/{segment}")
    public ResponseEntity<SegmentDetailResponse> getSegmentDetail(
            @RequestParam UUID businessId,
            @PathVariable String segment,
            @RequestParam(required = false, defaultValue = "6") Integer months) {
        SegmentDistributionResponse data = analyticsService.getSegmentDistribution(businessId, months);
        if (data.getSegments() != null && data.getSegments().containsKey(segment)) {
            return ResponseEntity.ok(SegmentDetailResponse.builder()
                    .segment(segment)
                    .data(data.getSegments().get(segment))
                    .build());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/insights")
    public ResponseEntity<BusinessInsightsResponse> getInsights(
            @RequestParam UUID businessId,
            @RequestParam(required = false, defaultValue = "3") Integer months) {
        return ResponseEntity.ok(insightsService.getInsights(businessId, months));
    }

    @GetMapping("/insights/ai")
    public ResponseEntity<String> getAIInsights(
            @RequestParam UUID businessId,
            @RequestParam(required = false, defaultValue = "3") Integer months) {
        return ResponseEntity.ok(insightsService.getAIInsights(businessId, months));
    }
}