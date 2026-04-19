package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TourForScheduleResponse {
    UUID idTour;
    String tourName;
    String description;
    Integer durationMinutes;
    BigDecimal adultPrice;
    BigDecimal childPrice;
    String meetingPoint;
    String status;
}