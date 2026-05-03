package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Request.TransportTicketSearchRequest;
import com.example.java_service.Models.TransportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransportTicketSearchRepository {
    Page<TransportTicket> searchWithKeyword(TransportTicketSearchRequest request, Pageable pageable);
}