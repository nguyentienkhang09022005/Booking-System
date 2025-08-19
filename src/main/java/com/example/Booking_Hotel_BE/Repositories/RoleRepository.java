package com.example.Booking_Hotel_BE.Repositories;

import com.example.Booking_Hotel_BE.Models.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Roles, String> {
}
