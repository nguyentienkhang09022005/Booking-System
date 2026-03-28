package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.BookingTour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingTourRepository extends JpaRepository<BookingTour, UUID>
{
    List<BookingTour> findByUsers_IdUser(UUID idUser);

    List<BookingTour> findByTour_Users_IdUser(UUID idUser);

    @Query("SELECT b FROM BookingTour b WHERE b.tour.idTour = :idTour AND MONTH(b.createdAt) = :month AND YEAR(b.createdAt) = :year")
    List<BookingTour> findByTourAndMonthYear(@Param("idTour") UUID idTour, @Param("month") int month, @Param("year") int year);
}