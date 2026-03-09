package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TourRepository extends JpaRepository<Tour, UUID>
{
    // Lấy danh sách các tour đã được phê duyệt
    List<Tour> findByApprovedTrue();

    // Lấy danh sách các tour chưa được phê duyệt
    List<Tour> findByApprovedFalse();

    // Lấy danh sách các tour do một user cụ thể tạo ra
    List<Tour> findByUsers_IdUser(UUID idUser);
}