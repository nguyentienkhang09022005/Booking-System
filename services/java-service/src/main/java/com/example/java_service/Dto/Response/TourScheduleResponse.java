package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TourScheduleResponse
{
    private UUID idSchedule;
    private LocalDate departureDate;
    private LocalTime departureTime;
    private String arrivalLocation;
    private String description;
}