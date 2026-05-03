package com.example.java_service.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrendResponse {
    private PeriodDTO period;
    private List<MonthlyDataDTO> monthlyData;
    private Map<String, List<Integer>> chartData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodDTO {
        private String from;
        private String to;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyDataDTO {
        private String month;
        private Integer bookings;
        private Long revenue;
        private Map<String, Integer> segments;
    }
}