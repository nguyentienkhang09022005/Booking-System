package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccommodationUpdateRequest
{
    String accommodationName;
    String street;
    String ward;
    String city;
    String description;
    Boolean approved;
    List<String> listImageAccommodation;
    List<AccommodationCreationRequest.FacilityCategoryRequest> facilityCategories;
    List<AccommodationCreationRequest.GeneralRegulationRequest> generalRegulations;
}
