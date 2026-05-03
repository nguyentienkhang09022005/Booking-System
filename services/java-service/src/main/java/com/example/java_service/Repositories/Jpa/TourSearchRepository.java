package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Request.TourSearchRequest;
import com.example.java_service.Models.Tour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TourSearchRepository {
    Page<Tour> searchWithKeyword(TourSearchRequest request, Pageable pageable);
}