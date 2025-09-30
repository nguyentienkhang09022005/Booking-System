package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.BookingRequest;
import com.example.java_service.Models.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper
{
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "bookingDate", ignore = true)
    Booking toBooking(BookingRequest request);
}
