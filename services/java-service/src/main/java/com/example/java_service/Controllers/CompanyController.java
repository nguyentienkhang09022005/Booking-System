package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.CompanyBankAccountRequest;
import com.example.java_service.Dto.Request.WithdrawalRequestCreation;
import com.example.java_service.Dto.Response.*;
import com.example.java_service.Services.CompanyBalanceService;
import jakarta.validation.Valid;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompanyController {

    CompanyBalanceService companyBalanceService;

    @GetMapping("/balance")
    public ApiResponse<CompanyBalanceResponse> getBalance(@RequestParam UUID idUser) {
        return ApiResponse.<CompanyBalanceResponse>builder()
                .message("Lấy số dư công ty thành công!")
                .data(companyBalanceService.getBalance(idUser))
                .build();
    }

    @GetMapping("/bank-account")
    public ApiResponse<CompanyBankAccountResponse> getBankAccount(@RequestParam UUID idUser) {
        CompanyBankAccountResponse response = companyBalanceService.getBankAccount(idUser);
        if (response == null) {
            return ApiResponse.<CompanyBankAccountResponse>builder()
                    .message("Chưa có thông tin tài khoản ngân hàng")
                    .data(null)
                    .build();
        }
        return ApiResponse.<CompanyBankAccountResponse>builder()
                .message("Lấy thông tin tài khoản ngân hàng thành công!")
                .data(response)
                .build();
    }

    @PutMapping("/bank-account")
    public ApiResponse<CompanyBankAccountResponse> updateBankAccount(
            @RequestParam UUID idUser,
            @Valid @RequestBody CompanyBankAccountRequest request) {
        return ApiResponse.<CompanyBankAccountResponse>builder()
                .message("Cập nhật tài khoản ngân hàng thành công!")
                .data(companyBalanceService.updateBankAccount(idUser, request))
                .build();
    }

    @PostMapping("/withdrawal/request")
    public ApiResponse<WithdrawalRequestResponse> createWithdrawalRequest(
            @RequestParam UUID idUser,
            @Valid @RequestBody WithdrawalRequestCreation request) {
        try {
            WithdrawalRequestResponse response = companyBalanceService.createWithdrawalRequest(idUser, request);
            return ApiResponse.<WithdrawalRequestResponse>builder()
                    .message("Tạo yêu cầu rút tiền thành công!")
                    .data(response)
                    .build();
        } catch (RuntimeException e) {
            return ApiResponse.<WithdrawalRequestResponse>builder()
                    .message(e.getMessage())
                    .data(null)
                    .build();
        }
    }

    @GetMapping("/withdrawal/requests")
    public ApiResponse<List<WithdrawalRequestResponse>> getMyWithdrawalRequests(@RequestParam UUID idUser) {
        return ApiResponse.<List<WithdrawalRequestResponse>>builder()
                .message("Lấy lịch sử yêu cầu rút tiền thành công!")
                .data(companyBalanceService.getUserWithdrawalRequests(idUser))
                .build();
    }
}