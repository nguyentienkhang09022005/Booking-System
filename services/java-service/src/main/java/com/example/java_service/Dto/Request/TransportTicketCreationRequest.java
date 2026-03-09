package com.example.java_service.Dto.Request;

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
public class TransportTicketCreationRequest
{
    UUID idUser;
    String ticketType;
    String ticketClass;
    String providerName;
    String departureLocation;
    String arrivalLocation;
    LocalDateTime departureTime;
    LocalDateTime arrivalTime;
    BigDecimal price;
}
