package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.BookingRequest;
import com.example.java_service.Dto.Request.BookingUpdateRequest;
import com.example.java_service.Dto.Response.BookingResponse;
import com.example.java_service.Models.Booking;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BookingMapper
{
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "bookingDate", ignore = true)
    Booking toBooking(BookingRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateBookingFromRequest(BookingUpdateRequest request, @MappingTarget Booking booking);

    @Mapping(target = "bookingRoomInBookingResponses", ignore = true)
    @Mapping(target = "bookingServiceInBookingResponses", ignore = true)
    BookingResponse toBookingResponse(Booking booking);
}
