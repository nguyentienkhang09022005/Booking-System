package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.FacilityCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FacilityCategoryRepository extends JpaRepository<FacilityCategory, UUID>
{
    List<FacilityCategory> findByHotel_IdHotel(UUID hotelIdHotel);

    void deleteByHotel_IdHotel(UUID idHotel);
}
