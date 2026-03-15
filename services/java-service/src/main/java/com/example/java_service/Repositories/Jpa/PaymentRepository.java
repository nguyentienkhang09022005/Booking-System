package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID>
{
    Boolean existsByBooking_IdBookingAndStatus(UUID idBooking, String status);

    Payment findByBooking_IdBooking(UUID idBooking);

    List<Payment> findByBooking_IdBookingIn(List<UUID> bookingIds);

    void deleteByBooking_IdBooking(UUID idBooking);

    boolean existsByBookingTour_IdBookingTourAndStatus(UUID idBookingTour, String status);

    void deleteByBookingTour_IdBookingTour(UUID idBookingTour);

    Payment findByBookingTour_IdBookingTour(UUID idBookingTour);

    List<Payment> findByBookingTour_IdBookingTourIn(List<UUID> idBookingTours);

    Payment findByBookingTransport_IdBookingTransport(UUID idBookingTransport);

    List<Payment> findByBookingTransport_IdBookingTransportIn(List<UUID> idBookingTransportIds);

    boolean existsByBookingTransport_IdBookingTransportAndStatus(UUID idBookingTransport, String status);
}
