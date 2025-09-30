package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.BookingRoomRequest;
import com.example.java_service.Models.BookingRoom;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingRoomMapper
{
    @Mapping(target = "priceBookingRoom", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "room", ignore = true)
    BookingRoom toBookingRoom(BookingRoomRequest request);
}
