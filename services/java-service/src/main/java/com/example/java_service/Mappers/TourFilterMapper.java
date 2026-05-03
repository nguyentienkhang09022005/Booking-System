package com.example.java_service.Mappers;

import com.example.java_service.Dto.Response.TourFilterResponse;
import com.example.java_service.Models.Tour;

import java.util.List;

public interface TourFilterMapper {

    TourFilterResponse toResponse(Tour tour, List<String> imageUrls);
}