package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest
{
    String role;
    String typeUser;
    String firstName;
    String lastName;
    String phone;
    String gender;
    LocalDate dob;
    String address;
    String email;
    String avatar;
}
