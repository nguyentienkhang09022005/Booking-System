package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingResponse
{
    UUID idBooking;
    LocalDateTime bookingDate;
    Date checkinDate;
    Date checkoutDate;
    String status;
    BigDecimal totalPrice;
    List<BookingRoomInBookingResponse> bookingRoomInBookingResponses;
    List<BookingServiceInBookingResponse> bookingServiceInBookingResponses;
}

class BookingRoomInBookingResponse
{
    UUID idBookingRoom;
    BigDecimal priceBookingRoom;
    Integer quantity;
    List<RoomResponse> roomResponses;
}

class BookingServiceInBookingResponse
{
    UUID idBookingService;
    Integer quantity;
    BigDecimal priceBookingService;
    List<ServiceResponse> serviceResponses;
}
