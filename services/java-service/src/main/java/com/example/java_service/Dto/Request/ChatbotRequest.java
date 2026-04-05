package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddPointRequest
{
    UUID idUser;
    BigDecimal amount;
    UUID idBooking;
    UUID idBookingTour;
    UUID idBookingTransport;
}