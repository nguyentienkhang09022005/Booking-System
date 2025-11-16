package com.example.java_service.Services;

import com.example.java_service.Dto.Request.AppliedVoucherRequest;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.AppliedVoucherMapper;
import com.example.java_service.Models.AppliedVoucher;
import com.example.java_service.Models.Booking;
import com.example.java_service.Models.Users;
import com.example.java_service.Models.Voucher;
import com.example.java_service.Repositories.Jpa.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AppliedVoucherService
{
    AppliedVoucherRepository appliedVoucherRepository;
    UserRepository userRepository;
    VoucherRepository voucherRepository;
    BookingRepository bookingRepository;
    AppliedVoucherMapper appliedVoucherMapper;

    // Hàm tạo voucher đã sử dụng bởi user
    public void saveAppliedVoucher(AppliedVoucherRequest appliedVoucherRequest, UUID idBooking)
    {
        Users user = userRepository.findById(appliedVoucherRequest.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Voucher voucher = voucherRepository.findById(appliedVoucherRequest.getIdVoucher()).orElseThrow(
                () -> new ApiException(ErrorCode.VOUCHER_NOT_FOUND)
        );

        Booking booking = bookingRepository.findById(idBooking).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        AppliedVoucher appliedVoucher = AppliedVoucher.builder()
                .user(user)
                .voucher(voucher)
                .booking(booking)
                .build();
        appliedVoucherRepository.save(appliedVoucher);
    }

    public void deleteAppliedVoucher(UUID idBooking)
    {
        bookingRepository.findById(idBooking).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        appliedVoucherRepository.deleteAppliedVoucherByBooking_IdBooking(idBooking);
    }
}
