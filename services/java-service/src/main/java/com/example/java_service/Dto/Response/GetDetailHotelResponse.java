package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GetDetailHotelResponse
{
    UUID idHotel;
    OwnerHotelResponse ownerHotelResponse;
    String hotelName;
    String street;
    String ward;
    String city;
    String description;
    Boolean favorite;
    Integer allEvaluates;
    Double averageStar;
    List<String> imageHotel;
    List<EvaluateResponse> evaluates;
}
