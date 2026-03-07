package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GetListAccommodationResponse
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
    Boolean favorite;
    Integer allEvaluates;
    Double averageStar;
    Boolean approved;
    List<String> imageAccommodationUrl;
}
