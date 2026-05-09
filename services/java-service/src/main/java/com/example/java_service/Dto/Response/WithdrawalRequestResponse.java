package com.example.java_service.Dto.Response;

import com.example.java_service.Models.WithdrawalRequest.WithdrawalStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WithdrawalRequestResponse {
    UUID idRequest;
    UUID idUser;
    BigDecimal amount;
    WithdrawalStatus status;
    String bankSnapshot;
    String companyNote;
    String adminNote;
    String transferReference;
    LocalDateTime createdAt;
    LocalDateTime processedAt;
    LocalDateTime completedAt;
}