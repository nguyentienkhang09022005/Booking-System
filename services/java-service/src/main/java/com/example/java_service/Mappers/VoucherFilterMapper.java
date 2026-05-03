package com.example.java_service.Mappers;

import com.example.java_service.Dto.Response.VoucherFilterResponse;
import com.example.java_service.Models.Voucher;

public interface VoucherFilterMapper {

    VoucherFilterResponse toResponse(Voucher voucher, String imageUrl);
}