package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateItineraryRequest {
    String destination;
    LocalDate startDate;
    LocalDate endDate;
    BigDecimal budget;
    String currency;
    String notes;
    String status;
}