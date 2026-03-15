package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingTourUpdateRequest
{
    Integer quantityAdult;
    Integer quantityChild;
    String pickupLocation;
    String languageSelected;
    String specialRequests;
    String paymentMethod;
    String payerAddress;
    String status;
}
