package com.example.java_service.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportTicketFilterRequest {
    private String keyword;
    private String ticketType;
    private String ticketClass;
    private String providerName;
    private String departureLocation;
    private String arrivalLocation;
    private LocalDateTime departureFrom;
    private LocalDateTime departureTo;
    private LocalDateTime arrivalFrom;
    private LocalDateTime arrivalTo;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer minAvailableSlots;
    private String sortBy;
    private String sortDir;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
