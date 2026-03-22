package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoyaltyTransactionCreationRequest
{
    UUID idUser;
    Integer points;
    String description;

    UUID idBooking;
    UUID idBookingTour;
    UUID idBookingTransport;
}