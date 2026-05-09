package com.example.java_service.Services;

import com.example.java_service.Dto.Request.*;
import com.example.java_service.Dto.Response.*;
import com.example.java_service.Models.*;
import com.example.java_service.Models.WithdrawalRequest.WithdrawalStatus;
import com.example.java_service.Repositories.Jpa.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompanyBalanceService {

    CompanyBalanceRepository companyBalanceRepository;
    CompanyBankAccountRepository companyBankAccountRepository;
    WithdrawalRequestRepository withdrawalRequestRepository;
    UserRepository userRepository;
    ObjectMapper objectMapper;

    @Transactional
    public void addEarnings(PaymentSuccessNotification notification) {
        log.info("Adding earnings for user {} amount {}", notification.getIdUser(), notification.getAmount());

        // Find or create company balance
        CompanyBalance balance = companyBalanceRepository.findByIdUserForUpdate(notification.getIdUser())
                .orElseGet(() -> {
                    Users user = userRepository.findById(notification.getIdUser()).orElse(null);
                    if (user == null) {
                        log.warn("User not found: {}", notification.getIdUser());
                        return null;
                    }
                    CompanyBalance newBalance = CompanyBalance.builder()
                            .users(user)
                            .build();
                    return companyBalanceRepository.save(newBalance);
                });

        if (balance == null) {
            log.error("Could not find or create balance for user: {}", notification.getIdUser());
            return;
        }

        // Add to total earned
        companyBalanceRepository.addToTotalEarned(notification.getIdUser(), notification.getAmount());
        log.info("Successfully added {} to total_earned for user {}", notification.getAmount(), notification.getIdUser());
    }

    public CompanyBalanceResponse getBalance(UUID idUser) {
        CompanyBalance balance = companyBalanceRepository.findByIdUserForUpdate(idUser)
                .orElse(CompanyBalance.builder()
                        .build());

        return CompanyBalanceResponse.builder()
                .idUser(idUser)
                .totalEarned(balance.getTotalEarned())
                .totalWithdrawn(balance.getTotalWithdrawn())
                .lockedAmount(balance.getLockedAmount())
                .availableBalance(balance.getAvailableBalance())
                .build();
    }

    @Transactional
    public CompanyBankAccountResponse updateBankAccount(UUID idUser, CompanyBankAccountRequest request) {
        Users user = userRepository.findById(idUser)
                .orElseThrow(() -> new RuntimeException("User not found: " + idUser));

        CompanyBankAccount bankAccount = companyBankAccountRepository.findById(idUser)
                .orElse(CompanyBankAccount.builder()
                        .users(user)
                        .build());

        bankAccount.setBankName(request.getBankName());
        bankAccount.setAccountNumber(request.getAccountNumber());
        bankAccount.setAccountHolderName(request.getAccountHolderName());
        bankAccount.setBranch(request.getBranch());

        bankAccount = companyBankAccountRepository.save(bankAccount);

        return CompanyBankAccountResponse.builder()
                .idUser(bankAccount.getIdUser())
                .bankName(bankAccount.getBankName())
                .accountNumber(bankAccount.getAccountNumber())
                .accountHolderName(bankAccount.getAccountHolderName())
                .branch(bankAccount.getBranch())
                .updatedAt(bankAccount.getUpdatedAt())
                .build();
    }

    public CompanyBankAccountResponse getBankAccount(UUID idUser) {
        CompanyBankAccount bankAccount = companyBankAccountRepository.findById(idUser)
                .orElse(null);

        if (bankAccount == null) {
            return null;
        }

        return CompanyBankAccountResponse.builder()
                .idUser(bankAccount.getIdUser())
                .bankName(bankAccount.getBankName())
                .accountNumber(bankAccount.getAccountNumber())
                .accountHolderName(bankAccount.getAccountHolderName())
                .branch(bankAccount.getBranch())
                .updatedAt(bankAccount.getUpdatedAt())
                .build();
    }

    @Transactional
    public WithdrawalRequestResponse createWithdrawalRequest(UUID idUser, WithdrawalRequestCreation request) {
        // Check if bank account exists
        CompanyBankAccount bankAccount = companyBankAccountRepository.findById(idUser)
                .orElseThrow(() -> new RuntimeException("Vui lòng cập nhật thông tin tài khoản ngân hàng trước"));

        // Check available balance
        CompanyBalance balance = companyBalanceRepository.findByIdUserForUpdate(idUser)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy số dư công ty"));

        BigDecimal availableBalance = balance.getAvailableBalance();
        if (availableBalance.compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Số dư không đủ. Số dư khả dụng: " + availableBalance);
        }

        // Create bank snapshot
        String bankSnapshot;
        try {
            bankSnapshot = objectMapper.writeValueAsString(CompanyBankAccountResponse.builder()
                    .idUser(bankAccount.getIdUser())
                    .bankName(bankAccount.getBankName())
                    .accountNumber(bankAccount.getAccountNumber())
                    .accountHolderName(bankAccount.getAccountHolderName())
                    .branch(bankAccount.getBranch())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create bank snapshot");
        }

        Users user = userRepository.findById(idUser)
                .orElseThrow(() -> new RuntimeException("User not found: " + idUser));

        WithdrawalRequest withdrawalRequest = WithdrawalRequest.builder()
                .users(user)
                .amount(request.getAmount())
                .status(WithdrawalStatus.PENDING)
                .bankSnapshot(bankSnapshot)
                .companyNote(request.getCompanyNote())
                .build();

        withdrawalRequest = withdrawalRequestRepository.save(withdrawalRequest);

        return mapToResponse(withdrawalRequest);
    }

    public List<WithdrawalRequestResponse> getUserWithdrawalRequests(UUID idUser) {
        return withdrawalRequestRepository.findByUsers_IdUserOrderByCreatedAtDesc(idUser)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<WithdrawalRequestResponse> getWithdrawalRequests(String status, int page, int size) {
        Page<WithdrawalRequest> withdrawalPage;

        if (status == null || status.isEmpty()) {
            withdrawalPage = withdrawalRequestRepository.findAllByOrderByCreatedAtDesc(
                    org.springframework.data.domain.PageRequest.of(page, size));
        } else {
            try {
                WithdrawalStatus withdrawalStatus = WithdrawalStatus.valueOf(status.toUpperCase());
                withdrawalPage = withdrawalRequestRepository.findByStatus(withdrawalStatus,
                        org.springframework.data.domain.PageRequest.of(page, size));
            } catch (IllegalArgumentException e) {
                withdrawalPage = withdrawalRequestRepository.findAllByOrderByCreatedAtDesc(
                        org.springframework.data.domain.PageRequest.of(page, size));
            }
        }

        return withdrawalPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveWithdrawal(UUID withdrawalId, String transferReference) {
        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal request not found: " + withdrawalId));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể phê duyệt yêu cầu đang chờ");
        }

        // Update withdrawal status
        withdrawal.setStatus(WithdrawalStatus.COMPLETED);
        withdrawal.setTransferReference(transferReference);
        withdrawal.setProcessedAt(LocalDateTime.now());
        withdrawal.setCompletedAt(LocalDateTime.now());
        withdrawalRequestRepository.save(withdrawal);

        // Update company balance - add to total_withdrawn
        UUID userId = withdrawal.getUsers().getIdUser();
        CompanyBalance balance = companyBalanceRepository.findByIdUserForUpdate(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy số dư công ty"));

        balance.setTotalWithdrawn(balance.getTotalWithdrawn().add(withdrawal.getAmount()));
        companyBalanceRepository.save(balance);

        log.info("Approved withdrawal {} for user {}, amount {}",
                withdrawalId, userId, withdrawal.getAmount());
    }

    @Transactional
    public void rejectWithdrawal(UUID withdrawalId, String adminNote) {
        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal request not found: " + withdrawalId));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể từ chối yêu cầu đang chờ");
        }

        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawal.setAdminNote(adminNote);
        withdrawal.setProcessedAt(LocalDateTime.now());
        withdrawalRequestRepository.save(withdrawal);

        UUID userId = withdrawal.getUsers().getIdUser();
        log.info("Rejected withdrawal {} for user {}, reason: {}",
                withdrawalId, userId, adminNote);
    }

    private WithdrawalRequestResponse mapToResponse(WithdrawalRequest withdrawal) {
        return WithdrawalRequestResponse.builder()
                .idRequest(withdrawal.getIdRequest())
                .idUser(withdrawal.getUsers().getIdUser())
                .amount(withdrawal.getAmount())
                .status(withdrawal.getStatus())
                .bankSnapshot(withdrawal.getBankSnapshot())
                .companyNote(withdrawal.getCompanyNote())
                .adminNote(withdrawal.getAdminNote())
                .transferReference(withdrawal.getTransferReference())
                .createdAt(withdrawal.getCreatedAt())
                .processedAt(withdrawal.getProcessedAt())
                .completedAt(withdrawal.getCompletedAt())
                .build();
    }
}