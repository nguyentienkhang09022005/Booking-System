package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.BookingService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookingServiceRepository extends JpaRepository<BookingService, UUID>
{
    void deleteByBooking_IdBooking(UUID idBooking);
}
