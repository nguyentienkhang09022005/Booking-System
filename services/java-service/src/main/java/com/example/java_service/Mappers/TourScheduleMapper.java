package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.TourScheduleRequest;
import com.example.java_service.Dto.Response.TourScheduleResponse;
import com.example.java_service.Models.TourSchedule;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TourScheduleMapper
{
    TourSchedule toTourSchedule(TourScheduleRequest request);

    TourScheduleResponse toTourScheduleResponse(TourSchedule tourSchedule);
}