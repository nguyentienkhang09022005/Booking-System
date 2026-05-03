package com.example.java_service.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourFilterRequest {
    private String keyword;
    private BigDecimal minAdultPrice;
    private BigDecimal maxAdultPrice;
    private BigDecimal minChildPrice;
    private BigDecimal maxChildPrice;
    private Integer minDuration;
    private Integer maxDuration;
    private String durationType;
    private String languages;
    private String departureLocation;
    private String destination;
    private String tourType;
    private String difficulty;
    private String sortBy;
    private String sortDir;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
