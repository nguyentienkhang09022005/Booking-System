package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TourResponse
{
    UUID idTour;
    OwnerTourResponse ownerTourResponse;
    String tourName;
    String description;
    Integer durationMinutes;
    String meetingPoint;
    String dropOffPoint;
    String cancellationPolicy;
    String languages;

    List<String> inclusions;
    List<String> exclusions;
    List<String> additionalInfo;
    List<TourScheduleResponse> tourScheduleResponses;

    String status;
    Boolean approved;
    LocalDateTime createdAt;

    List<String> imageTourUrl;
}