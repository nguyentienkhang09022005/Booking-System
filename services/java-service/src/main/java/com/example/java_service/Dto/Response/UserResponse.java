package com.example.java_service.Dto.Response;

import com.example.java_service.Models.Roles;
import com.example.java_service.Models.TypeUser;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

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
    LocalDateTime createAt;
    String avatar;
}
