package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.HotelFacility;
import com.example.java_service.Models.HotelFacilityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelFacilityRepository extends JpaRepository<HotelFacility, HotelFacilityId>
{

}
