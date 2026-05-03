package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Request.TransportTicketFilterRequest;
import com.example.java_service.Models.TransportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Repository;

@Repository
public interface TransportTicketFilterRepository {

    Page<TransportTicket> findAllWithFilters(TransportTicketFilterRequest request, Pageable pageable);
}
