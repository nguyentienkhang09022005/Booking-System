package com.example.java_service.Dto.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TourCreationRequest
{
    @NotNull(message = "ID User không được để trống")
    UUID idUser;

    @NotBlank(message = "Tên tour không được để trống")
    String tourName;

    String description;

    @Min(value = 1, message = "Thời lượng tour (giờ) phải lớn hơn 0")
    Integer durationMinutes;

    String meetingPoint;
    String dropOffPoint;
    String cancellationPolicy;
    String languages;

    List<String> inclusions;
    List<String> exclusions;
    List<String> additionalInfo;
    List<TourScheduleRequest> tourScheduleRequests;

    List<String> imageTourUrl;
}