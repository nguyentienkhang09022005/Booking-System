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

