package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompanyBalanceResponse {
    UUID idUser;
    BigDecimal totalEarned;
    BigDecimal totalWithdrawn;
    BigDecimal lockedAmount;
    BigDecimal availableBalance;
}