package com.example.java_service.Repositories;

import com.example.java_service.Models.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, UUID>
{

}
