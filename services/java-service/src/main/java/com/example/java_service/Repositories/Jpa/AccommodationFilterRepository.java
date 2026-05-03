package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Request.AccommodationFilterRequest;
import com.example.java_service.Models.Accommodation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

@Repository
public interface AccommodationFilterRepository {

    Page<Accommodation> findAllWithFilters(AccommodationFilterRequest request, Pageable pageable);

    List<Accommodation> findAllByIdIn(List<UUID> ids);
}
