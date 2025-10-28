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
    @Query("SELECT bs.service.idService FROM BookingService bs WHERE bs.booking.idBooking = :idBooking")
    List<UUID> findServiceIdsByBookingId(@Param("idBooking") UUID idBooking);

    void deleteByBooking_IdBooking(UUID idBooking);

    @Query("SELECT bs FROM BookingService bs JOIN FETCH bs.service WHERE bs.booking.idBooking IN :bookingIds")
    List<BookingService> findAllBookingServiceByBookingIds(@Param("bookingIds") List<UUID> bookingIds);

    List<BookingService> findByBooking_IdBooking(UUID idBooking);
}
