package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RevenueCreatedEvent {
    UUID idExpense;
    UUID idBooking;
    UUID idAccommodation;
    BigDecimal amount;
    int month;
    int year;
}

