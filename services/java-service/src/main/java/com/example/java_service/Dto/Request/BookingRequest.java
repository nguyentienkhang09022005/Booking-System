package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingRequest
{
    UUID idUser;
    Date checkinDate;
    Date checkoutDate;
    List<BookingRoomRequest> bookingRooms;
    List<BookingServiceRequest> bookingServices;
}
