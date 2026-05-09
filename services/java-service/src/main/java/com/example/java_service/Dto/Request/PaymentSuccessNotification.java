package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentSuccessNotification {
    UUID idPayment;
    UUID idUser;
    BigDecimal amount;
    String paymentMethod;
    UUID idBooking;
    UUID idBookingTour;
    UUID idBookingTransport;
}