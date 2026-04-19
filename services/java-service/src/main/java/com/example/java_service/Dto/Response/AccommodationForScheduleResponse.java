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
public class AccommodationForScheduleResponse {
    UUID idAccommodation;
    String accommodationName;
    String city;
    String province;
    String address;
    BigDecimal minPrice;
    BigDecimal maxPrice;
    Double averageStar;
}