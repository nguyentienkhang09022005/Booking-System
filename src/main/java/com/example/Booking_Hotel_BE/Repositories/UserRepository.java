package com.example.Booking_Hotel_BE.Repositories;

import com.example.Booking_Hotel_BE.Models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Users, Integer> {
}
