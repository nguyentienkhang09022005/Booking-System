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
public class AccommodationFilterRequest {
    private String keyword;
    private String accommodationType;
    private String province;
    private String city;
    private String ward;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sortBy;
    private String sortDir;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
