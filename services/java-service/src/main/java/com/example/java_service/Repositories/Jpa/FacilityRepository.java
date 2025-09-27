package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, UUID>
{

}
