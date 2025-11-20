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
public class BookingListResponse
{
    UUID idBooking;
    LocalDateTime bookingDate;
    LocalDateTime checkinDate;
    LocalDateTime checkoutDate;
    String status;
    BigDecimal totalPrice;
}
