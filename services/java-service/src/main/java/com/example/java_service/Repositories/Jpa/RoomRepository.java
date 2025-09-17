package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID>
{
    Page<Room> findByHotel_IdHotel(UUID idHotel, Pageable pageable);
}
