package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.WithdrawalApprovalRequest;
import com.example.java_service.Dto.Request.WithdrawalRejectionRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.WithdrawalRequestResponse;
import com.example.java_service.Services.CompanyBalanceService;
import jakarta.validation.Valid;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/withdrawal")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminWithdrawalController {

    CompanyBalanceService companyBalanceService;

    @GetMapping("/requests")
    public ApiResponse<List<WithdrawalRequestResponse>> getWithdrawalRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<List<WithdrawalRequestResponse>>builder()
                .message("Lấy danh sách yêu cầu rút tiền thành công!")
                .data(companyBalanceService.getWithdrawalRequests(status, page, size))
                .build();
    }

    @PutMapping("/requests/{id}/approve")
    public ApiResponse<Void> approveWithdrawal(
            @PathVariable UUID id,
            @Valid @RequestBody WithdrawalApprovalRequest request) {
        try {
            companyBalanceService.approveWithdrawal(id, request.getTransferReference());
            return ApiResponse.<Void>builder()
                    .message("Phê duyệt yêu cầu rút tiền thành công!")
                    .build();
        } catch (RuntimeException e) {
            return ApiResponse.<Void>builder()
                    .message(e.getMessage())
                    .build();
        }
    }

    @PutMapping("/requests/{id}/reject")
    public ApiResponse<Void> rejectWithdrawal(
            @PathVariable UUID id,
            @RequestBody WithdrawalRejectionRequest request) {
        try {
            companyBalanceService.rejectWithdrawal(id, request.getAdminNote());
            return ApiResponse.<Void>builder()
                    .message("Từ chối yêu cầu rút tiền thành công!")
                    .build();
        } catch (RuntimeException e) {
            return ApiResponse.<Void>builder()
                    .message(e.getMessage())
                    .build();
        }
    }
}