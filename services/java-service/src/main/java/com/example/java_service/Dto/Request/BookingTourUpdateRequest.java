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
public class BookingTourUpdateRequest
{
    UUID idUser;
    Integer quantityAdult;
    Integer quantityChild;
    String pickupLocation;
    String languageSelected;
    String specialRequests;
    String paymentMethod;
    String payerAddress;
    String status;
    List<UserVoucherCreationRequest> userVoucherRequests;
}
