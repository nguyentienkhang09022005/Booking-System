package com.example.java_service.Dto.Response;

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
public class VoucherViewResponse
{
    UUID idVoucher;
    String nameVoucher;
    BigDecimal discountPercent;
    LocalDate startDate;
    LocalDate endDate;
    Integer quantity;
    String conditions;
    Boolean approved;
    Boolean isSaved;
    String urlImage;
}
