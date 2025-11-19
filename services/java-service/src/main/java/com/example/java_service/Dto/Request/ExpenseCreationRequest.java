package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseCreationRequest
{
    UUID idUser;
    UUID idHotel;
    String expenseTitle;
    BigDecimal amount;
    String description;
    String department;
}
