package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
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
    UUID idHotel;
    LocalDateTime checkinDate;
    LocalDateTime checkoutDate;
    List<BookingRoomRequest> bookingRooms;
    List<BookingServiceRequest> bookingServices;
    List<UserVoucherRequest> userVoucherRequests;
}
