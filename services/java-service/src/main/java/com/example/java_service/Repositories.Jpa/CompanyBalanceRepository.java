package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.CompanyBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyBalanceRepository extends JpaRepository<CompanyBalance, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cb FROM CompanyBalance cb WHERE cb.idUser = :idUser")
    Optional<CompanyBalance> findByIdUserForUpdate(@Param("idUser") UUID idUser);

    @Modifying
    @Query("UPDATE CompanyBalance cb SET cb.totalEarned = cb.totalEarned + :amount WHERE cb.idUser = :idUser")
    int addToTotalEarned(@Param("idUser") UUID idUser, @Param("amount") java.math.BigDecimal amount);
}