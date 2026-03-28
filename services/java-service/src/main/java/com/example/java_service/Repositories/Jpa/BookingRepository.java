package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID>
{
    List<Booking> findByAccommodation_IdAccommodation(UUID idAccommodation);

    List<Booking> findByUsers_IdUser(UUID idUser);

    @Query("SELECT b FROM Booking b WHERE b.accommodation.idAccommodation = :idAccommodation AND MONTH(b.bookingDate) = :month AND YEAR(b.bookingDate) = :year")
    List<Booking> findByAccommodationAndMonthYear(@Param("idAccommodation") UUID idAccommodation, @Param("month") int month, @Param("year") int year);
}
