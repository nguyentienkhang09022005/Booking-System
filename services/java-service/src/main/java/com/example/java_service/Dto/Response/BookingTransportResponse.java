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
public class BookingTourResponse
{
    UUID idBookingTour;
    Integer quantityAdult;
    Integer quantityChild;
    BigDecimal priceBookingTour;
    String pickupLocation;
    String languageSelected;
    String specialRequests;
    PaymentInBookingResponse paymentInBookingResponse;
    LocalDateTime createdAt;
    TourResponse tourResponse;
}
