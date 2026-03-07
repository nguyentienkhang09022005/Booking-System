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
    UUID idAccommodation;
    LocalDateTime checkinDate;
    LocalDateTime checkoutDate;
    String currency;
    String note;
    List<BookingRoomRequest> bookingRooms;
    List<BookingServiceRequest> bookingServices;
    List<UserVoucherRequest> userVoucherRequests;

    // Payment method: "ZaloPay" (default) or "VNDStablecoin"
    String paymentMethod;
    // Required when paymentMethod == "VNDStablecoin"
    String payerAddress;
}
