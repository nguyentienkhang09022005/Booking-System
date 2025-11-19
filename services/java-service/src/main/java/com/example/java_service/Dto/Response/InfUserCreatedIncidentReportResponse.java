package com.example.java_service.Dto.Response;

import com.example.java_service.Models.Roles;
import com.example.java_service.Models.TypeUser;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InfUserCreatedIncidentReportResponse
{
    UUID idUser;
    String firstName;
    String lastName;
    String phone;
    String gender;
    LocalDate dob;
    String address;
    String email;
}
