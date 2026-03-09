package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.TransportTicketCreationRequest;
import com.example.java_service.Dto.Request.TransportTicketUpdateRequest;
import com.example.java_service.Dto.Response.TransportTicketResponse;
import com.example.java_service.Models.TransportTicket;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TransportTicketMapper
{
    @Mapping(target = "users", ignore = true)
    TransportTicket toTransportTicket(TransportTicketCreationRequest request);

    @Mapping(target = "ownerTransportTicketResponse", ignore = true)
    TransportTicketResponse toTransportTicketResponse(TransportTicket transportTicket);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateTransportTicketFromRequest(TransportTicketUpdateRequest request, @MappingTarget TransportTicket transportTicket);
}