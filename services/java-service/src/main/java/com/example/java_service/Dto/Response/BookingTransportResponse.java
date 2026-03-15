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
public class BookingTransportResponse
{

    UUID idBookingTransport;
    Integer quantityTickets;
    BigDecimal priceBookingTransport;
    String seatNumbers;
    String status;
    PaymentInBookingResponse paymentInBookingResponse;
    LocalDateTime createdAt;
}
