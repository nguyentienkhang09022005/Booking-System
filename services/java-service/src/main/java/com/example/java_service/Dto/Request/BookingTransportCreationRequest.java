package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingTransportCreationRequest
{
    UUID idTicket;
    UUID idUser;
    Integer quantityTickets;
    String seatNumbers;
    List<UserVoucherCreationRequest> userVoucherRequests;
}
