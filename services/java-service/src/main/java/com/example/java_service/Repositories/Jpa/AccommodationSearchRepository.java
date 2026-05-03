package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Request.AccommodationSearchRequest;
import com.example.java_service.Models.Accommodation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AccommodationSearchRepository {
    Page<Accommodation> searchWithKeyword(AccommodationSearchRequest request, Pageable pageable);
    List<Accommodation> findAllByIdIn(List<UUID> ids);
}