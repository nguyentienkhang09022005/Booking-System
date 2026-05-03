package com.example.java_service.Events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCompletedEvent {
    private UUID userId;
    private String bookingType;
    private UUID bookingId;
}