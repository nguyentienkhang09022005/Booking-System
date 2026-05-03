package com.example.java_service.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherFilterRequest {
    private String keyword;
    private String code;
    private BigDecimal minDiscountPercent;
    private BigDecimal maxDiscountPercent;
    private BigDecimal minDiscountAmount;
    private BigDecimal maxDiscountAmount;
    private String sponsorType;
    private String applyTo;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Integer minQuantity;
    private UUID accommodationId;
    private Boolean expireSoon;
    private Boolean newVoucher;
    private String sortBy;
    private String sortDir;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
