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
public class TourResponse
{
    UUID idTour;
    OwnerTourResponse ownerTourResponse;
    String tourName;
    String description;
    Integer durationDays;
    BigDecimal pricePerPerson;
    String status;
    Boolean approved;
    LocalDateTime createdAt;
}
