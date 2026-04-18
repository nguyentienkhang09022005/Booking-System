package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateItineraryItemRequest {
    String itemType;
    LocalDate scheduledDate;
    LocalTime startTime;
    LocalTime endTime;
    String suggestedName;
    String suggestedLocation;
    BigDecimal suggestedPrice;
    Integer estimatedDurationMinutes;
    Integer displayOrder;
}
