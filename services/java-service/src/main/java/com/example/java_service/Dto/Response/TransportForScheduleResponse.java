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
public class TransportForScheduleResponse {
    UUID idTicket;
    String providerName;
    String ticketType;
    String departureLocation;
    String arrivalLocation;
    BigDecimal price;
    LocalDateTime departureTime;
    LocalDateTime arrivalTime;
    Integer durationMinutes;
    String status;
}