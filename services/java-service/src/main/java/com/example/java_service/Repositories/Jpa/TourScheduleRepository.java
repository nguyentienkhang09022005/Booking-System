package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.TourSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TourScheduleRepository extends JpaRepository<TourSchedule, UUID>
{
    void deleteByTour_IdTour(UUID idTour);
}