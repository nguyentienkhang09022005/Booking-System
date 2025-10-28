package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.BookingRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRoomRepository extends JpaRepository<BookingRoom, UUID>
{
    @Query("SELECT br.room.idRoom FROM BookingRoom br WHERE br.booking.idBooking = :idBooking")
    List<UUID> findRoomIdsByBookingId(@Param("idBooking") UUID idBooking);

    void deleteByBooking_IdBooking(UUID idBooking);

    @Query("SELECT br FROM BookingRoom br JOIN FETCH br.room WHERE br.booking.idBooking IN :bookingIds")
    List<BookingRoom> findAllBookingRoomByBookingIds(@Param("bookingIds") List<UUID> bookingIds);

    List<BookingRoom> findByBooking_IdBooking(UUID idBooking);

}
