package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Response.SimpleItemResponse;
import com.example.java_service.Models.TransportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransportTicketRepository extends JpaRepository<TransportTicket, UUID>
{
    List<TransportTicket> findByApprovedTrue();

    List<TransportTicket> findByApprovedFalse();

    List<TransportTicket> findByUsers_IdUser(UUID idUser);

    @Query("SELECT new com.example.java_service.Dto.Response.SimpleItemResponse(t.idTicket, CONCAT(t.providerName, ' - ', t.ticketType)) FROM TransportTicket t WHERE t.approved = true")
    List<SimpleItemResponse> findAllSimpleTransportTickets();

    Page<TransportTicket> findByApprovedTrue(Pageable pageable);
}