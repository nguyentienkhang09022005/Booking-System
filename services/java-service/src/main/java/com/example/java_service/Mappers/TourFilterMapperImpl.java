package com.example.java_service.Mappers;

import com.example.java_service.Dto.Response.TourFilterResponse;
import com.example.java_service.Dto.Response.TourScheduleResponse;
import com.example.java_service.Models.Tour;
import com.example.java_service.Models.TourSchedule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TourFilterMapperImpl implements TourFilterMapper {

    @Override
    public TourFilterResponse toResponse(Tour tour, List<String> imageUrls) {
        List<TourScheduleResponse> scheduleResponses = tour.getTourSchedules() != null
                ? tour.getTourSchedules().stream()
                        .map(this::toScheduleResponse)
                        .toList()
                : List.of();

        return TourFilterResponse.builder()
                .idTour(tour.getIdTour())
                .tourName(tour.getTourName())
                .description(tour.getDescription())
                .durationMinutes(tour.getDurationMinutes())
                .meetingPoint(tour.getMeetingPoint())
                .dropOffPoint(tour.getDropOffPoint())
                .adultPrice(tour.getAdultPrice())
                .childPrice(tour.getChildPrice())
                .languages(tour.getLanguages())
                .inclusions(tour.getInclusions())
                .exclusions(tour.getExclusions())
                .additionalInfo(tour.getAdditionalInfo())
                .status(tour.getStatus())
                .createdAt(tour.getCreatedAt())
                .tourSchedules(scheduleResponses)
                .imageUrls(imageUrls != null ? imageUrls : List.of())
                .build();
    }

    private TourScheduleResponse toScheduleResponse(TourSchedule schedule) {
        return TourScheduleResponse.builder()
                .idSchedule(schedule.getIdSchedule())
                .departureDate(schedule.getDepartureDate())
                .departureTime(schedule.getDepartureTime())
                .arrivalLocation(schedule.getArrivalLocation())
                .description(schedule.getDescription())
                .build();
    }
}