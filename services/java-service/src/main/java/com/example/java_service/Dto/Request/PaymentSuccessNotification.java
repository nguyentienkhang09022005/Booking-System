package com.example.java_service.Dto.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("id_payment")
    UUID idPayment;

    @JsonProperty("id_user")
    UUID idUser;

    BigDecimal amount;

    @JsonProperty("payment_method")
    String paymentMethod;

    @JsonProperty("id_booking")
    UUID idBooking;

    @JsonProperty("id_booking_tour")
    UUID idBookingTour;

    @JsonProperty("id_booking_transport")
    UUID idBookingTransport;
}