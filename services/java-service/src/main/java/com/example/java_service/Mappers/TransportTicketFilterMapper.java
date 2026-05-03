package com.example.java_service.Mappers;

import com.example.java_service.Dto.Response.TransportTicketFilterResponse;
import com.example.java_service.Models.TransportTicket;

public interface TransportTicketFilterMapper {

    TransportTicketFilterResponse toResponse(TransportTicket ticket);
}