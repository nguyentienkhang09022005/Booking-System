package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccommodationUpdateRequest
{
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
    List<String> listImageAccommodation;
    List<AccommodationCreationRequest.FacilityCategoryRequest> facilityCategories;
    List<AccommodationCreationRequest.GeneralRegulationRequest> generalRegulations;
}
