package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingServiceInBookingResponse
{
    UUID idBookingService;
    Integer quantity;
    BigDecimal priceBookingService;
    List<ServiceResponse> serviceResponses;
}


