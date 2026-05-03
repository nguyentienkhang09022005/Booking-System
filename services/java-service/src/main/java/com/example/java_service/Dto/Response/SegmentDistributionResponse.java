package com.example.java_service.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentDistributionResponse {
    private Integer periodMonths;
    private Map<String, SegmentStatsDTO> segments;
    private Long totalCustomers;
    private Long totalRevenue;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentStatsDTO {
        private Long count;
        private Long totalRevenue;
        private Integer avgBookingValue;
        private Double percentage;
    }
}