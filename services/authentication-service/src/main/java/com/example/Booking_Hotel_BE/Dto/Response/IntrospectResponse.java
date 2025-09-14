package com.example.Booking_Hotel_BE.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntrospectResponse
{
    boolean valid;
    UUID idUser;
    String role;
}
