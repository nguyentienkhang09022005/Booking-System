package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoyaltyTransactionResponse
{
    UUID idLoyaltyTrans;
    Integer points;
    String description;
    LocalDateTime createdAt;

    UUID idUser;
    UUID idBooking;
    UUID idBookingTour;
    UUID idBookingTransport;
}