package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TourOverviewResponse
{
    UUID idTour;
    String tourName;
    Integer durationMinutes;
    BigDecimal adultPrice;
    BigDecimal childPrice;
    String status;
    Boolean approved;
    LocalDateTime createdAt;
    String thumbnailImage;
    OwnerTourResponse ownerTourResponse;
}