package com.example.Booking_Hotel_BE.Dto.Response;

import com.example.Booking_Hotel_BE.Models.Roles;
import com.example.Booking_Hotel_BE.Models.TypeUser;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse
{
    UUID idUser;
    Roles role;
    TypeUser typeUser;
    String firstName;
    String lastName;
    String phone;
    String gender;
    LocalDate dob;
    String address;
    String email;
    BigDecimal totalAmount;
//
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
//    LocalDateTime createAt;
}
