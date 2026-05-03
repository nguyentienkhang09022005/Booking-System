package com.example.java_service.Mappers;

import com.example.java_service.Dto.Response.VoucherFilterResponse;
import com.example.java_service.Models.Voucher;
import org.springframework.stereotype.Component;

@Component
public class VoucherFilterMapperImpl implements VoucherFilterMapper {

    @Override
    public VoucherFilterResponse toResponse(Voucher voucher, String imageUrl) {
        return VoucherFilterResponse.builder()
                .idVoucher(voucher.getIdVoucher())
                .nameVoucher(voucher.getNameVoucher())
                .discountPercent(voucher.getDiscountPercent())
                .minDiscountAmount(voucher.getMinDiscountAmount())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .sponsorType(voucher.getSponsorType())
                .applyTo(voucher.getApplyTo())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .quantity(voucher.getQuantity())
                .conditions(voucher.getConditions())
                .createAt(voucher.getCreateAt())
                .imageUrl(imageUrl)
                .build();
    }
}