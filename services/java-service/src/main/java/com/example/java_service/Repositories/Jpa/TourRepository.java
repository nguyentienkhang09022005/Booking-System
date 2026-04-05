package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Response.SimpleItemResponse;
import com.example.java_service.Models.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TourRepository extends JpaRepository<Tour, UUID>
{
    @Query("SELECT DISTINCT t FROM Tour t LEFT JOIN FETCH t.tourSchedules WHERE t.approved = true")
    List<Tour> findByApprovedTrue();

    @Query("SELECT DISTINCT t FROM Tour t LEFT JOIN FETCH t.tourSchedules WHERE t.approved = false")
    List<Tour> findByApprovedFalse();

    @Query("SELECT DISTINCT t FROM Tour t LEFT JOIN FETCH t.tourSchedules WHERE t.users.idUser = :idUser")
    List<Tour> findByUsers_IdUser(@Param("idUser") UUID idUser);

    @Query("SELECT DISTINCT t FROM Tour t LEFT JOIN FETCH t.tourSchedules")
    List<Tour> findAllToursWithSchedules();

    @Query("SELECT new com.example.java_service.Dto.Response.SimpleItemResponse(t.idTour, t.tourName) FROM Tour t WHERE t.approved = true")
    List<SimpleItemResponse> findAllSimpleTours();

    @Query("SELECT new com.example.java_service.Dto.Response.SimpleItemResponse(t.idTour, t.tourName) FROM Tour t WHERE t.approved = true AND LOWER(t.tourName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<SimpleItemResponse> searchByName(@Param("keyword") String keyword);

    List<Tour> findByIdTourIn(List<UUID> ids);
}