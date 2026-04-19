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
    List<Room> findByAccommodation_IdAccommodation(UUID idAccommodation);

    // Update status nếu số lượng bằng 0
    @Modifying
    @Query(""" 
    UPDATE Room r
    SET r.status = :updateStatus
    WHERE r.idRoom = :idRoom AND r.quantity = 0""")
    int updateStatusIfOutOfRoom(@Param("idRoom") UUID idRoom,
                                @Param("updateStatus") String updateStatus);

    // Giảm số lượng room khi bookingroom
    @Modifying
    @Query("""
    UPDATE Room r
    SET r.quantity = r.quantity - :bookedQuantity
    WHERE r.idRoom = :idRoom AND r.quantity >= :bookedQuantity""")
    int decreaseRoomQuantity(@Param("idRoom") UUID idRoom,
                             @Param("bookedQuantity") int bookedQuantity);

    // Tăng số lượng khi cập nhật, xóa, hủy booking
    @Modifying
    @Query("""
    UPDATE Room r
    SET r.quantity = r.quantity + :quantity
    WHERE r.idRoom = :idRoom""")
    int restoreRoomQuantity(@Param("idRoom") UUID idRoom, @Param("quantity") int quantity);

    // Update status nếu số lượng lớn hơn 0
    @Modifying
    @Query("""
    UPDATE Room r
    SET r.status = :status
    WHERE r.idRoom = :idRoom AND r.quantity > 0""")
    int updateStatusIfAvailable(@Param("idRoom") UUID idRoom, @Param("status") String status);

    // Cập nhật trạng thái cho Room
    @Modifying
    @Query("""
    UPDATE Room r
    SET r.status = :status
    WHERE r.idRoom IN :roomIds""")
    void updateStatusForRoomIds(@Param("roomIds") List<UUID> idRoom, @Param("status") String status);

    List<Room> findByRoomNameContainingIgnoreCase(String roomName);

    List<Room> findByAccommodation_IdAccommodationAndStatus(UUID idAccommodation, String status);

    List<Room> findByTypeRoom_TypeRoomContainingIgnoreCase(String typeRoom);

}
