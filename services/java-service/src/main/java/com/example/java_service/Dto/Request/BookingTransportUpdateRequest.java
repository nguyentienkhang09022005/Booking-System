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
public class BookingTransportUpdateRequest
{
    UUID idUser;
    Integer quantityTickets;
    String seatNumbers;
    String status;
    List<UserVoucherCreationRequest> userVoucherRequests;
}
