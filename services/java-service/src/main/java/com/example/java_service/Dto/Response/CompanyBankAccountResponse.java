package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompanyBankAccountResponse {
    UUID idUser;
    String bankName;
    String accountNumber;
    String accountHolderName;
    String branch;
    LocalDateTime updatedAt;
}