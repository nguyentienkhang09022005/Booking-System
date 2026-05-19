package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.BulletinBoard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BulletinBoardRepository extends JpaRepository<BulletinBoard, UUID> {

    @Query("SELECT b FROM BulletinBoard b JOIN FETCH b.users WHERE b.status = :status")
    List<BulletinBoard> findByStatusWithUser(@Param("status") String status);

    @Query("SELECT b FROM BulletinBoard b JOIN FETCH b.users WHERE b.status = 'APPROVED' " +
           "AND (b.targetAudience = :audience OR b.targetAudience = 'ALL') " +
           "AND (b.startDate IS NULL OR b.startDate <= :now) " +
           "AND (b.endDate IS NULL OR b.endDate >= :now) " +
           "ORDER BY b.priority DESC, b.createdAt DESC")
    List<BulletinBoard> findActiveByAudience(@Param("audience") String audience, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM BulletinBoard b JOIN FETCH b.users WHERE b.status = :status ORDER BY b.createdAt DESC")
    Page<BulletinBoard> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(b) FROM BulletinBoard b WHERE b.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT b FROM BulletinBoard b JOIN FETCH b.users WHERE b.users.idUser = :userId ORDER BY b.createdAt DESC")
    List<BulletinBoard> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT b FROM BulletinBoard b JOIN FETCH b.users WHERE b.users.idUser = :userId AND b.status = :status ORDER BY b.createdAt DESC")
    List<BulletinBoard> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") String status);
}