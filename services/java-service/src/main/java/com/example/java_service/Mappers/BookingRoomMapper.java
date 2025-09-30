package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.BookingServiceRequest;
import com.example.java_service.Models.BookingService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingServiceMapper
{
    @Mapping(target = "priceBookingService", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "service", ignore = true)
    BookingService toBookingService(BookingServiceRequest bookingServiceRequest);
}
