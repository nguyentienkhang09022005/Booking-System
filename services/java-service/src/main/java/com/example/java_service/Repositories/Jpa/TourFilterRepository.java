package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Request.TourFilterRequest;
import com.example.java_service.Models.Tour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Repository;

@Repository
public interface TourFilterRepository {

    Page<Tour> findAllWithFilters(TourFilterRequest request, Pageable pageable);
}
