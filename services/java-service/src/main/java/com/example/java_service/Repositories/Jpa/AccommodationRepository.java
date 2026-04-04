package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Response.SimpleItemResponse;
import com.example.java_service.Models.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT new com.example.java_service.Dto.Response.SimpleItemResponse(a.idAccommodation, a.accommodationName) FROM Accommodation a WHERE a.approved = true")
    List<SimpleItemResponse> findAllSimpleAccommodations();

    @Query("SELECT a FROM Accommodation a LEFT JOIN FETCH a.users WHERE a.idAccommodation = :id")
    Accommodation findByIdWithUsers(@Param("id") UUID idAccommodation);
}
