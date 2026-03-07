package com.example.java_service.Dto.Request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
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
public class AccommodationCreationRequest
{
    UUID idUser;
    String accommodationName;
    String accommodationType;
    String street;
    String ward;
    String city;
    String province;
    LocalTime checkinTime;
    LocalTime checkoutTime;
    String description;
    List<String> listImageAccommodation;
    List<FacilityCategoryRequest> facilityCategories;
    List<GeneralRegulationRequest> generalRegulations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FacilityCategoryRequest {
        String categoryName;
        List<FacilityRequest> facilities;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FacilityRequest {
        String facilityName;
        Boolean isFree;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GeneralRegulationRequest {
        String regulationName;
        String regulationContent;
    }
}
