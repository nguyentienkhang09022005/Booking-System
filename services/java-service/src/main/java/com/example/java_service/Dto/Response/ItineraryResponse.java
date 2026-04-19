package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItineraryResponse {
    String idItinerary;
    String idUser;
    String destination;
    LocalDate startDate;
    LocalDate endDate;
    BigDecimal budget;
    String status;
    BigDecimal totalEstimatedCost;
    String currency;
    String notes;
    LocalDateTime createdAt;
    List<ItineraryItemResponse> items;
    List<TourForScheduleResponse> suggestedTours;
}