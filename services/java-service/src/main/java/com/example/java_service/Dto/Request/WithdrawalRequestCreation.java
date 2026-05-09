package com.example.java_service.Dto.Request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WithdrawalRequestCreation {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100000", message = "Minimum withdrawal amount is 100,000 VND")
    BigDecimal amount;

    String companyNote;
}