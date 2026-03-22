package com.example.java_service.Mappers;

import com.example.java_service.Dto.Response.LoyaltyTransactionResponse;
import com.example.java_service.Models.LoyaltyTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoyaltyTransactionMapper
{

    @Mapping(source = "users.idUser", target = "idUser")
    @Mapping(source = "booking.idBooking", target = "idBooking")
    @Mapping(source = "bookingTour.idBookingTour", target = "idBookingTour")
    @Mapping(source = "bookingTransport.idBookingTransport", target = "idBookingTransport")
    LoyaltyTransactionResponse toResponse(LoyaltyTransaction transaction);
}