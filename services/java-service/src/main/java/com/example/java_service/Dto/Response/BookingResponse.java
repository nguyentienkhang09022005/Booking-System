package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
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
    LocalDateTime checkinDate;
    LocalDateTime checkoutDate;
    String note;
    String status;
    BigDecimal totalPrice;
    PaymentInBookingResponse paymentInBookingResponse;
    List<BookingRoomInBookingResponse> bookingRoomInBookingResponses;
    List<BookingServiceInBookingResponse> bookingServiceInBookingResponses;
    List<VoucherViewResponse> voucherViewResponses;
}
