package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, UUID>
{
    Boolean existsByUsers_IdUser(UUID idUser);

    List<Hotel> findByUsers_IdUser(UUID idUser);
}
