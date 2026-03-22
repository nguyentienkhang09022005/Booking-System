package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.LoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, UUID> {

    List<LoyaltyTransaction> findByUsers_IdUserOrderByCreatedAtDesc(UUID idUser);

    List<LoyaltyTransaction> findByBooking_IdBookingOrderByCreatedAtDesc(UUID idBooking);

    List<LoyaltyTransaction> findByBookingTour_IdBookingTourOrderByCreatedAtDesc(UUID idBookingTour);

    List<LoyaltyTransaction> findByBookingTransport_IdBookingTransportOrderByCreatedAtDesc(UUID idBookingTransport);
}