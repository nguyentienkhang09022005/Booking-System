package com.example.Booking_Hotel_BE.Repositories;

import com.example.Booking_Hotel_BE.Models.OTP;
import com.example.Booking_Hotel_BE.Models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OTP, Integer> {
    Optional<OTP> findByOtpAndUser(Integer otp, Users user);
}
