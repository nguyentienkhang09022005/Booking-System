package com.example.java_service.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherFilterResponse {
    private UUID idVoucher;
    private String nameVoucher;
    private BigDecimal discountPercent;
    private BigDecimal minDiscountAmount;
    private BigDecimal maxDiscountAmount;
    private String sponsorType;
    private String applyTo;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer quantity;
    private String conditions;
    private LocalDateTime createAt;
    private String imageUrl;
}
