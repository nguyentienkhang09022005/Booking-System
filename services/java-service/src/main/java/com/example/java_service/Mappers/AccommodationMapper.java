package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.AccommodationCreationRequest;
import com.example.java_service.Dto.Request.AccommodationUpdateRequest;
import com.example.java_service.Dto.Response.GetDetailAccommodationResponse;
import com.example.java_service.Dto.Response.GetListAccommodationResponse;
import com.example.java_service.Dto.Response.AccommodationResponse;
import com.example.java_service.Models.Accommodation;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AccommodationMapper
{
    @Mapping(target = "users", ignore = true)
    Accommodation toAccommodation(AccommodationCreationRequest request);

    @Mapping(target = "ownerAccommodationResponse", ignore = true)
    AccommodationResponse toAccommodationResponse(Accommodation accommodation);

    @Mapping(target = "ownerAccommodationResponse", ignore = true)
    @Mapping(target = "favorite", ignore = true)
    @Mapping(target = "evaluates", ignore = true)
    @Mapping(target = "averageStar", ignore = true)
    @Mapping(target = "allEvaluates", ignore = true)
    GetDetailAccommodationResponse toGetDetailAccommodationResponse(Accommodation accommodation);

    @Mapping(target = "imageAccommodationUrl", ignore = true)
    @Mapping(target = "favorite", ignore = true)
    @Mapping(target = "averageStar", ignore = true)
    @Mapping(target = "allEvaluates", ignore = true)
    GetListAccommodationResponse toGetListAccommodationResponse(Accommodation accommodation);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateAccommodationFromRequest(AccommodationUpdateRequest request, @MappingTarget Accommodation accommodation);
}
