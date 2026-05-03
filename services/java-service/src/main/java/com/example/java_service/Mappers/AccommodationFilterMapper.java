package com.example.java_service.Mappers;

import com.example.java_service.Dto.Response.AccommodationFilterResponse;
import com.example.java_service.Models.Accommodation;
import com.example.java_service.Models.Images;

import java.util.List;

public interface AccommodationFilterMapper {

    AccommodationFilterResponse toResponse(Accommodation accommodation, List<Images> images);
}