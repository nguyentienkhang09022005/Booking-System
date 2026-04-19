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
public class TourBasicInfoResponse
{
    UUID idTour;
    String tourName;
    String description;
    BigDecimal adultPrice;
    BigDecimal childPrice;
    Integer durationMinutes;
    String meetingPoint;
    String dropOffPoint;
    String cancellationPolicy;
    String languages;
    String status;
}