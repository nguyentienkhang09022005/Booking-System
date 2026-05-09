package com.example.java_service.Dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompanyBankAccountRequest {
    @NotBlank(message = "Bank name is required")
    String bankName;

    @NotBlank(message = "Account number is required")
    String accountNumber;

    @NotBlank(message = "Account holder name is required")
    String accountHolderName;

    String branch;
}