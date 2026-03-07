package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation, UUID>
{
    Boolean existsByUsers_IdUser(UUID idUser);

    Boolean existsByIdAccommodation(UUID idAccommodation);

    List<Accommodation> findByUsers_IdUser(UUID idUser);

    List<Accommodation> findAllByApprovedTrue();

    List<Accommodation> findAllByApprovedFalse();
}
