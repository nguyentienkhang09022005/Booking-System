package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, UUID> {
    List<Itinerary> findByUsers_IdUserOrderByCreatedAtDesc(UUID idUser);
}