package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.BookingTransport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingTransportTicketRepository extends JpaRepository<BookingTransport, UUID> {
    List<BookingTransport> findByUsers_IdUser(UUID idUser);

    List<BookingTransport> findByTransportTicket_Users_IdUser(UUID idUser);

    @Query("SELECT b FROM BookingTransport b WHERE b.transportTicket.idTicket = :idTicket AND MONTH(b.createdAt) = :month AND YEAR(b.createdAt) = :year")
    List<BookingTransport> findByTransportTicketAndMonthYear(@Param("idTicket") UUID idTicket, @Param("month") int month, @Param("year") int year);
}