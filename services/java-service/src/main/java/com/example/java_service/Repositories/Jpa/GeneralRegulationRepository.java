package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.GeneralRegulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GeneralRegulationRepository extends JpaRepository<GeneralRegulation, UUID>
{
    List<GeneralRegulation> findByHotel_IdHotel(UUID idHotel);
}
