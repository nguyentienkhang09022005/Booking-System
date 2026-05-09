package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.WithdrawalRequest;
import com.example.java_service.Models.WithdrawalRequest.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, UUID> {

    List<WithdrawalRequest> findByUsers_IdUserOrderByCreatedAtDesc(UUID idUser);

    Page<WithdrawalRequest> findByStatusOrderByCreatedAtDesc(WithdrawalStatus status, Pageable pageable);

    Page<WithdrawalRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WithdrawalRequest w " +
           "WHERE w.users.idUser = :idUser AND w.status = 'PENDING'")
    java.math.BigDecimal sumPendingAmountByUser(@Param("idUser") UUID idUser);

    @Query("SELECT w FROM WithdrawalRequest w WHERE w.status = :status ORDER BY w.createdAt DESC")
    Page<WithdrawalRequest> findByStatus(@Param("status") WithdrawalStatus status, Pageable pageable);
}