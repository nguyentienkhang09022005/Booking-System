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
}
