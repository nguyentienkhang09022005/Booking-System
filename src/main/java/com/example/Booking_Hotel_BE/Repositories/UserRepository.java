package com.example.Booking_Hotel_BE.Repositories;

import com.example.Booking_Hotel_BE.Models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<Users, UUID> {
    Optional<Users> findByEmail(String email);
    Boolean existsByEmail(String email);
}
