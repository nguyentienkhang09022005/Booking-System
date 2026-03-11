package com.example.java_service.Dto.Request;

import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TourUpdateRequest
{
    String tourName;
    String description;

    @Min(value = 1, message = "Thời lượng tour (phút) phải lớn hơn 0")
    Integer durationMinutes;

    String meetingPoint;
    String dropOffPoint;
    String cancellationPolicy;
    String languages;
    String status;
    BigDecimal adultPrice;
    BigDecimal childPrice;

    List<String> inclusions;
    List<String> exclusions;
    List<String> additionalInfo;
    List<TourScheduleRequest> tourScheduleRequests;

    List<String> imageTourUrl;
}