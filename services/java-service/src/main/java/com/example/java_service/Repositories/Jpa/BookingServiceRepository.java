package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.BookingService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingServiceRepository extends JpaRepository<BookingService, UUID>
{
    void deleteByBooking_IdBooking(UUID idBooking);

    @Query("SELECT bs FROM BookingService bs JOIN FETCH bs.service WHERE bs.booking.idBooking IN :bookingIds")
    List<BookingService> findAllByBookingIds(@Param("bookingIds") List<UUID> bookingIds);

}
