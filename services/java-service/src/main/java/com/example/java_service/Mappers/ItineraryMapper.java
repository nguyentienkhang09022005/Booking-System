package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.CreateItineraryRequest;
import com.example.java_service.Dto.Request.UpdateItineraryRequest;
import com.example.java_service.Dto.Response.ItineraryResponse;
import com.example.java_service.Models.Itinerary;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ItineraryMapper {

    @Mapping(target = "idItinerary", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "totalEstimatedCost", ignore = true)
    @Mapping(target = "currency", constant = "VND")
    Itinerary toItinerary(CreateItineraryRequest request);

    @Mapping(target = "idItinerary", source = "idItinerary")
    @Mapping(target = "idUser", source = "users.idUser")
    ItineraryResponse toResponse(Itinerary itinerary);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "idItinerary", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateFromRequest(UpdateItineraryRequest request, @MappingTarget Itinerary itinerary);
}