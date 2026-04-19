package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccommodationBasicInfoResponse
{
    UUID idAccommodation;
    String accommodationName;
    String accommodationType;
    String street;
    String ward;
    String city;
    String province;
    LocalTime checkinTime;
    LocalTime checkoutTime;
    String description;
    Integer allEvaluates;
    Double averageStar;
    Boolean favorite;
}