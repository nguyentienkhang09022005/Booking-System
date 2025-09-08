package com.example.Booking_Hotel_BE.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordRequest
{
    String email;
    String newPassword;
    String confirmPassword;
    Integer otp;
}
