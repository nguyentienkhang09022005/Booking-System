package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.HotelCreationRequest;
import com.example.java_service.Models.Hotel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ResortMapper
{
    Hotel toHotel(HotelCreationRequest request);
}
