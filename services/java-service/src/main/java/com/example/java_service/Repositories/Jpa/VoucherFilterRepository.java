package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Request.VoucherFilterRequest;
import com.example.java_service.Models.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Repository;

@Repository
public interface VoucherFilterRepository {

    Page<Voucher> findAllWithFilters(VoucherFilterRequest request, Pageable pageable);
}
