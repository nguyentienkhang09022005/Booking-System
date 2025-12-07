package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseResponse
{
    UUID idExpense;
    String expenseTitle;
    BigDecimal amount;
    String description;
    String department;
    LocalDateTime createAt;
    InfUserCreatedExpenseResponse infUserCreatedExpenseResponse;
}
