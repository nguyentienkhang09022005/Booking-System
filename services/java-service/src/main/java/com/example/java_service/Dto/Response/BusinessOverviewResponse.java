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
public class BusinessOverviewResponse {
    private Integer periodMonths;
    private Long totalRevenue;
    private Integer totalBookings;
    private Integer avgBookingValue;
    private Integer totalProducts;
    private Integer accommodations;
    private Integer tours;
    private Integer transportTickets;
    private Map<String, Long> customerSegments;
}