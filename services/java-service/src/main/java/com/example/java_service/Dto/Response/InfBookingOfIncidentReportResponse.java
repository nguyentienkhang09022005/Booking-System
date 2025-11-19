package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.security.Timestamp;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InfBookingOfIncidentReportResponse
{
    UUID idBooking;
    LocalDateTime bookingDate;
    Date checkinDate;
    Date checkoutDate;
    String status;
    BigDecimal totalPrice;
}
