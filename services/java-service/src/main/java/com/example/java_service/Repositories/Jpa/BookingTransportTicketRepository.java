package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.BookingTransport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingTransportTicketRepository extends JpaRepository<BookingTransport, UUID> {
    List<BookingTransport> findByUsers_IdUser(UUID idUser);

    List<BookingTransport> findByTransportTicket_Users_IdUser(UUID idUser);
}