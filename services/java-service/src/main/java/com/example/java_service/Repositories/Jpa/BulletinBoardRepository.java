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
           "ORDER BY b.isFeatured DESC, b.createdAt DESC")
    List<BulletinBoard> findActiveByAudience(@Param("audience") String audience, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM BulletinBoard b JOIN FETCH b.users WHERE b.status = :status ORDER BY b.createdAt DESC")
    Page<BulletinBoard> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(b) FROM BulletinBoard b WHERE b.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT b FROM BulletinBoard b JOIN FETCH b.users WHERE b.users.idUser = :userId ORDER BY b.createdAt DESC")
    List<BulletinBoard> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT b FROM BulletinBoard b JOIN FETCH b.users WHERE b.users.idUser = :userId AND b.status = :status ORDER BY b.createdAt DESC")
    List<BulletinBoard> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") String status);

    // ===== 5 loại tin cho Customer =====

    // 1. Tin nổi bật (leaderboard) - isFeatured = true, lấy top N
    @Query("SELECT b FROM BulletinBoard b JOIN FETCH b.users WHERE b.status = 'APPROVED' " +
           "AND (b.targetAudience = :audience OR b.targetAudience = 'ALL') " +
           "AND b.isFeatured = true " +
           "AND (b.startDate IS NULL OR b.startDate <= :now) " +
           "AND (b.endDate IS NULL OR b.endDate >= :now) " +
           "ORDER BY b.createdAt DESC")
    List<BulletinBoard> findFeaturedLeaders(@Param("audience") String audience, @Param("now") LocalDateTime now);

    // 2. Tin nổi bật khác (grid 4 cột) - isFeatured = true, exclude leaderboard
    @Query("SELECT b FROM BulletinBoard b JOIN FETCH b.users WHERE b.status = 'APPROVED' " +
           "AND (b.targetAudience = :audience OR b.targetAudience = 'ALL') " +
           "AND b.isFeatured = true " +
           "AND b.idBulletin NOT IN :excludeIds " +
           "AND (b.startDate IS NULL OR b.startDate <= :now) " +
           "AND (b.endDate IS NULL OR b.endDate >= :now) " +
           "ORDER BY b.createdAt DESC")
    List<BulletinBoard> findFeaturedOthers(@Param("audience") String audience, @Param("now") LocalDateTime now,
                                          @Param("excludeIds") List<UUID> excludeIds);

    // 3. Tin mới (horizontal scroll) - sorted by createdAt DESC
    @Query("SELECT b FROM BulletinBoard b JOIN FETCH b.users WHERE b.status = 'APPROVED' " +
           "AND (b.targetAudience = :audience OR b.targetAudience = 'ALL') " +
           "AND (b.startDate IS NULL OR b.startDate <= :now) " +
           "AND (b.endDate IS NULL OR b.endDate >= :now) " +
           "ORDER BY b.createdAt DESC")
    List<BulletinBoard> findNewest(@Param("audience") String audience, @Param("now") LocalDateTime now);

    // 4. Tin sắp hết hạn (grid + icon cảnh báo) - endDate trong N ngày tới
    @Query("SELECT b FROM BulletinBoard b JOIN FETCH b.users WHERE b.status = 'APPROVED' " +
           "AND (b.targetAudience = :audience OR b.targetAudience = 'ALL') " +
           "AND b.endDate IS NOT NULL " +
           "AND b.endDate >= :now " +
           "AND b.endDate <= :threshold " +
           "AND (b.startDate IS NULL OR b.startDate <= :now) " +
           "ORDER BY b.endDate ASC")
    List<BulletinBoard> findExpiringSoon(@Param("audience") String audience, @Param("now") LocalDateTime now,
                                         @Param("threshold") LocalDateTime threshold);

    // 5. Tin thường (grid 3 cột) - exclude featured, newest, expiring
    @Query("SELECT b FROM BulletinBoard b JOIN FETCH b.users WHERE b.status = 'APPROVED' " +
           "AND (b.targetAudience = :audience OR b.targetAudience = 'ALL') " +
           "AND b.idBulletin NOT IN :excludeIds " +
           "AND (b.startDate IS NULL OR b.startDate <= :now) " +
           "AND (b.endDate IS NULL OR b.endDate >= :now) " +
           "ORDER BY b.createdAt DESC")
    List<BulletinBoard> findRegular(@Param("audience") String audience, @Param("now") LocalDateTime now,
                                    @Param("excludeIds") List<UUID> excludeIds);
}