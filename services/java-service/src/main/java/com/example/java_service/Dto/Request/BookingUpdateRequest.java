package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingUpdateRequest
{
    LocalDateTime checkinDate;
    LocalDateTime checkoutDate;
    String note;
    List<BookingRoomRequest> bookingRooms;
    List<BookingServiceRequest> bookingServices;
    List<UserVoucherRequest> userVoucherRequests;
}
