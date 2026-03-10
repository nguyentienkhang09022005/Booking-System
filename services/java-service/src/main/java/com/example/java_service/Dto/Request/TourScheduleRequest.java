package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TourScheduleRequest
{
    LocalDate departureDate;
    LocalTime departureTime;
    String arrivalLocation;
    String description;
}