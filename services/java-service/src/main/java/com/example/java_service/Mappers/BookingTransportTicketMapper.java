package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.BookingTransportUpdateRequest;
import com.example.java_service.Dto.Response.BookingTransportResponse;
import com.example.java_service.Dto.Response.PaymentInBookingResponse;
import com.example.java_service.Models.BookingTransport;
import com.example.java_service.Models.Payment;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BookingTransportTicketMapper
{
    BookingTransportResponse toBookingTransportResponse(BookingTransport bookingTransport);

    PaymentInBookingResponse toPaymentInBookingResponse(Payment payment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateBookingTransportFromRequest(BookingTransportUpdateRequest request, @MappingTarget BookingTransport bookingTransport);
}