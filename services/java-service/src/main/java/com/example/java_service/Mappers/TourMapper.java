package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.TourCreationRequest;
import com.example.java_service.Dto.Request.TourUpdateRequest;
import com.example.java_service.Dto.Response.TourResponse;
import com.example.java_service.Models.Tour;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TourMapper
{
    @Mapping(target = "users", ignore = true)
    Tour toTour(TourCreationRequest request);

    @Mapping(target = "ownerTourResponse", ignore = true)
    TourResponse toTourResponse(Tour tour);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateTourFromRequest(TourUpdateRequest request, @MappingTarget Tour tour);
}