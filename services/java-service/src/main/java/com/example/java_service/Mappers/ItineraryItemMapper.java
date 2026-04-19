package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.CreateItineraryItemRequest;
import com.example.java_service.Dto.Request.UpdateItineraryItemRequest;
import com.example.java_service.Dto.Response.ItineraryItemResponse;
import com.example.java_service.Models.Itinerary;
import com.example.java_service.Models.ItineraryItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ItineraryItemMapper {

    ItineraryItemResponse toResponse(ItineraryItem item);

    @Mapping(target = "idItem", ignore = true)
    @Mapping(target = "itinerary", source = "itinerary")
    @Mapping(target = "createdAt", ignore = true)
    ItineraryItem toEntity(CreateItineraryItemRequest request, Itinerary itinerary);

    @Mapping(target = "idItem", ignore = true)
    @Mapping(target = "itinerary", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateFromRequest(UpdateItineraryItemRequest request, @MappingTarget ItineraryItem item);
}
