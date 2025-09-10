package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.HotelCreationRequest;
import com.example.java_service.Dto.Response.GetListHotelResponse;
import com.example.java_service.Dto.Response.HotelCreationResponse;
import com.example.java_service.Models.Hotel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HotelMapper
{
    @Mapping(target = "users", ignore = true)
    Hotel toHotel(HotelCreationRequest request);

    @Mapping(target = "imageHotelUrl", ignore = true)
    HotelCreationResponse toHotelCreationResponse(Hotel hotel);

    @Mapping(target = "imageHotelUrl", ignore = true)
    @Mapping(target = "favorite", ignore = true)
    GetListHotelResponse toGetListHotelResponse(Hotel hotel);
}
