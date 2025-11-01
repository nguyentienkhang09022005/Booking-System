package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Models.AppliedVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AppliedVoucherRepository extends JpaRepository<AppliedVoucher, UUID>
{
}
