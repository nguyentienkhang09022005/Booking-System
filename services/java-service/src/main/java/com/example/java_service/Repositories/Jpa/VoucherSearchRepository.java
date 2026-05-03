package com.example.java_service.Repositories.Jpa;

import com.example.java_service.Dto.Request.VoucherSearchRequest;
import com.example.java_service.Models.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VoucherSearchRepository {
    Page<Voucher> searchWithKeyword(VoucherSearchRequest request, Pageable pageable);
}