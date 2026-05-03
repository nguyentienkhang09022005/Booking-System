package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID>
{
    List<Booking> findByAccommodation_IdAccommodation(UUID idAccommodation);

    List<Booking> findByUsers_IdUser(UUID idUser);

    @Query("SELECT b FROM Booking b WHERE b.accommodation.idAccommodation = :idAccommodation AND MONTH(b.bookingDate) = :month AND YEAR(b.bookingDate) = :year")
    List<Booking> findByAccommodationAndMonthYear(@Param("idAccommodation") UUID idAccommodation, @Param("month") int month, @Param("year") int year);

    @Query("""
        SELECT b FROM Booking b
        JOIN b.accommodation a
        JOIN a.users owner
        WHERE owner.idUser = :businessId AND b.status IN ('Success', 'Checked Out')
        AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate
        """)
    List<Booking> findByBusinessIdAndDateRange(
        @Param("businessId") UUID businessId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT b FROM Booking b
        JOIN b.users u
        JOIN b.accommodation a
        WHERE a.users.idUser = :businessId
        AND b.status IN ('Success', 'Checked Out')
        """)
    List<Booking> findCompletedBookingsByBusinessId(@Param("businessId") UUID businessId);

    @Query("""
        SELECT u.customerSegment, COUNT(b), SUM(b.totalPrice)
        FROM Booking b
        JOIN b.users u
        JOIN b.accommodation a
        WHERE a.users.idUser = :businessId
        AND b.status IN ('Success', 'Checked Out')
        AND u.customerSegment IS NOT NULL
        GROUP BY u.customerSegment
        """)
    List<Object[]> getSegmentDistributionByBusiness(@Param("businessId") UUID businessId);
}
