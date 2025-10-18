package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID>
{
    List<Room> findByHotel_IdHotel(UUID idHotel);

    @Modifying
    @Query("UPDATE Room room SET room.status = :newStatus WHERE room.idRoom = :idRoom AND room.status = :expectedStatus")
    int updateStatus(@Param("idRoom") UUID idRoom,
                     @Param("expectedStatus") String expectedStatus,
                     @Param("newStatus") String newStatus);

    @Modifying
    @Query("UPDATE Room r SET r.status = :status WHERE r.idRoom IN :roomIds")
    void updateStatusForRoomIds(@Param("roomIds") List<UUID> roomIds, @Param("status") String status);

}
