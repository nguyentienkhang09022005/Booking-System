package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelUpdateRequest
{
    String hotelName;
    String street;
    String ward;
    String city;
    String description;
    List<String> listImageHotel;
    List<HotelCreationRequest.FacilityCategoryRequest> facilityCategories;
    List<HotelCreationRequest.GeneralRegulationRequest> generalRegulations;
}
