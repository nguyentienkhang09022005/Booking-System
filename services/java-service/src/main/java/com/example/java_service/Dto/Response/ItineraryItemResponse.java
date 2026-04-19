package com.example.java_service.Dto.Response;

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
public class ItineraryItemResponse {
    String idItem;
    String itemType;
    LocalDate scheduledDate;
    LocalTime startTime;
    LocalTime endTime;
    String refId;
    String suggestedName;
    String suggestedLocation;
    BigDecimal suggestedPrice;
    Integer estimatedDurationMinutes;
//    String bookingStatus;
//    String bookingId;
//    String paymentStatus;
    Integer displayOrder;
}