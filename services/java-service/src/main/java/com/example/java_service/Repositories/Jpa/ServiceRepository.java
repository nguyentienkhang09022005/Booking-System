package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceRepository extends JpaRepository<Service, UUID>
{
    List<Service> findByAccommodation_IdAccommodation(UUID idAccommodation);

    // Update status nếu số lượng bằng 0
    @Modifying
    @Query(""" 
    UPDATE Service s
    SET s.status = :updateStatus
    WHERE s.idService = :idService AND s.quantity = 0""")
    int updateStatusIfOutOfService(@Param("idService") UUID idService,
                                @Param("updateStatus") String updateStatus);

    // Giảm số lượng room khi bookingservice
    @Modifying
    @Query("""
    UPDATE Service s
    SET s.quantity = s.quantity - :bookedQuantity
    WHERE s.idService = :idService AND s.quantity >= :bookedQuantity""")
    int decreaseServiceQuantity(@Param("idService") UUID idService,
                             @Param("bookedQuantity") int bookedQuantity);

    // Tăng số lượng khi cập nhật, xóa, hủy booking
    @Modifying
    @Query("""
    UPDATE Service s
    SET s.quantity = s.quantity + :quantity
    WHERE s.idService = :idService""")
    int restoreServiceQuantity(@Param("idService") UUID idService, @Param("quantity") int quantity);

    // Update status nếu số lượng lớn hơn 0
    @Modifying
    @Query("""
    UPDATE Service s
    SET s.status = :status
    WHERE s.idService = :idService AND s.quantity > 0""")
    int updateStatusIfAvailable(@Param("idService") UUID idService, @Param("status") String status);

    // Cập nhật trạng thái cho Service
    @Modifying
    @Query("""
    UPDATE Service s
    SET s.status = :status
    WHERE s.idService IN :ServiceIds""")
    void updateStatusForServiceIds(@Param("ServiceIds") List<UUID> idService, @Param("status") String status);

    List<Service> findByServiceNameContainingIgnoreCase(String name);

    List<Service> findByTypeService_TypeService(String typeService);

    List<Service> findByAccommodation_IdAccommodationAndStatus(UUID idAccommodation, String status);
}
