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
public class AccommodationResponse
{
    UUID idAccommodation;
    OwnerAccommodationResponse ownerAccommodationResponse;
    String accommodationName;
    String accommodationType;
    String street;
    String ward;
    String city;
    String province;
    LocalTime checkinTime;
    LocalTime checkoutTime;
    String description;
    Boolean approved;
    List<GetDetailAccommodationResponse.FacilityCategoryResponse> facilityCategoryResponses;
    List<GetDetailAccommodationResponse.GeneralRegulationResponse> generalRegulationResponses;
}
