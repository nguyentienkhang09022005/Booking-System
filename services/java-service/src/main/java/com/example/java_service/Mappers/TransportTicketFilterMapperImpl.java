package com.example.java_service.Mappers;

import com.example.java_service.Dto.Response.TransportTicketFilterResponse;
import com.example.java_service.Models.TransportTicket;
import org.springframework.stereotype.Component;

@Component
public class TransportTicketFilterMapperImpl implements TransportTicketFilterMapper {

    @Override
    public TransportTicketFilterResponse toResponse(TransportTicket ticket) {
        return TransportTicketFilterResponse.builder()
                .idTicket(ticket.getIdTicket())
                .ticketType(ticket.getTicketType())
                .ticketClass(ticket.getTicketClass())
                .providerName(ticket.getProviderName())
                .departureLocation(ticket.getDepartureLocation())
                .arrivalLocation(ticket.getArrivalLocation())
                .departureTime(ticket.getDepartureTime())
                .arrivalTime(ticket.getArrivalTime())
                .price(ticket.getPrice())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .build();
    }
}