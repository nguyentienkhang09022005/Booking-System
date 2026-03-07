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
public class GetDetailAccommodationResponse
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
    Boolean favorite;
    Integer allEvaluates;
    Double averageStar;
    List<String> imageAccommodation;
    List<GetRoomResponse> roomResponses;
    List<EvaluateResponse> evaluates;
    List<FacilityCategoryResponse> facilityCategoryResponses;
    List<GeneralRegulationResponse> generalRegulationResponses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FacilityCategoryResponse {
        UUID idCategory;
        String categoryName;
        List<FacilityResponse> facilities;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FacilityResponse {
        UUID idFacility;
        String facilityName;
        Boolean isFree;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GeneralRegulationResponse {
        UUID idRegulation;
        String regulationName;
        String regulationContent;
    }
}

