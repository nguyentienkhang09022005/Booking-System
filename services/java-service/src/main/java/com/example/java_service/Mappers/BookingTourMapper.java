package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.BookingTourCreationRequest;
import com.example.java_service.Dto.Request.BookingTourUpdateRequest;
import com.example.java_service.Dto.Response.BookingTourResponse;
import com.example.java_service.Dto.Response.PaymentInBookingResponse;
import com.example.java_service.Models.BookingTour;
import com.example.java_service.Models.Payment;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {TourMapper.class})
public interface BookingTourMapper {

    @Mapping(target = "idBookingTour", ignore = true)
    @Mapping(target = "tour", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "priceBookingTour", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    BookingTour toBookingTour(BookingTourCreationRequest request);

    @Mapping(source = "tour", target = "tourResponse")
    BookingTourResponse toBookingTourResponse(BookingTour bookingTour);

    @Mapping(source = "createAt", target = "createAt")
    PaymentInBookingResponse toPaymentInBookingResponse(Payment payment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "idBookingTour", ignore = true)
    @Mapping(target = "tour", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "priceBookingTour", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateBookingTourFromRequest(BookingTourUpdateRequest request, @MappingTarget BookingTour bookingTour);
}