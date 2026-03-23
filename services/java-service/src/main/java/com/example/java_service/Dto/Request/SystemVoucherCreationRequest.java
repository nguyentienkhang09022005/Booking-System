package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemVoucherCreationRequest
{
    String nameVoucher;
    BigDecimal discountPercent;
    LocalDate startDate;
    LocalDate endDate;
    Integer quantity;
    String conditions;
    BigDecimal minDiscountAmount;
    BigDecimal maxDiscountAmount;
    String applyTo;
    String urlImage;
}