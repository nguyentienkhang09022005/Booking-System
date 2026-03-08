package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherEditResponse
{
    UUID idVoucher;
    String nameVoucher;
    String code;
    BigDecimal discountPercent;
    LocalDate startDate;
    LocalDate endDate;
    Integer quantity;
    String conditions;
    LocalDateTime createAt;
    Boolean approved;
    BigDecimal minDiscountAmount;
    BigDecimal maxDiscountAmount;
    String sponsorType;
    String urlImage;
}
