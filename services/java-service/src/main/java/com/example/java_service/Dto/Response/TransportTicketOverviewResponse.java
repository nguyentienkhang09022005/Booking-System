package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransportTicketOverviewResponse
{
    UUID idTicket;
    String ticketType;
    String providerName;
    String departureLocation;
    String arrivalLocation;
    LocalDateTime departureTime;
    BigDecimal price;
    String status;
    Boolean approved;
    OwnerTransportTicketResponse ownerTransportTicketResponse;
}