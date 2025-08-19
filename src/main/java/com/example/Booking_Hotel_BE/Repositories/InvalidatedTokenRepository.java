package com.example.Booking_Hotel_BE.Repositories;

import com.example.Booking_Hotel_BE.Models.Invalidated_Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InvalidatedTokenRepository extends JpaRepository<Invalidated_Token, UUID> {
}
