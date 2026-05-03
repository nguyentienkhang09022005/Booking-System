package com.example.java_service.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourFilterResponse {
    private UUID idTour;
    private String tourName;
    private String description;
    private Integer durationMinutes;
    private String meetingPoint;
    private String dropOffPoint;
    private BigDecimal adultPrice;
    private BigDecimal childPrice;
    private String languages;
    private List<String> inclusions;
    private List<String> exclusions;
    private List<String> additionalInfo;
    private String status;
    private LocalDateTime createdAt;
    private List<TourScheduleResponse> tourSchedules;
    private List<String> imageUrls;
}