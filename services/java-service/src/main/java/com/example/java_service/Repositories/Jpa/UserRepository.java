package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<Users, UUID>
{
    Optional<Users> findByEmailOrPhone(String email, String phone);

    Optional<Users> findByEmail(String email);

    Optional<Users> findByPhone(String phone);

    Boolean existsByIdUser(UUID idUser);

    @Query("SELECT u FROM Users u WHERE u.customerSegment IS NULL")
    Page<Users> findUsersWithoutSegment(Pageable pageable);

    @Query("SELECT u FROM Users u WHERE u.customerSegment IS NULL")
    List<Users> findUsersWithoutSegmentList();

    @Modifying
    @Transactional
    @Query("UPDATE Users u SET u.customerSegment = :segment, u.segmentConfidence = :confidence, u.segmentAnalyzedAt = :analyzedAt WHERE u.idUser = :userId")
    void updateCustomerSegment(@Param("userId") UUID userId, @Param("segment") String segment, @Param("confidence") Double confidence, @Param("analyzedAt") LocalDateTime analyzedAt);

    @Query("SELECT COUNT(u) FROM Users u WHERE u.customerSegment = :segment")
    long countBySegment(@Param("segment") String segment);
}
