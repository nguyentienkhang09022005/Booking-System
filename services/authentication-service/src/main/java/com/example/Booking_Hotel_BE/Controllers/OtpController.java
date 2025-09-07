package com.example.Booking_Hotel_BE.Controllers;

import com.example.Booking_Hotel_BE.Dto.Request.ChangePasswordRequest;
import com.example.Booking_Hotel_BE.Dto.Request.CreateOtpRequest;
import com.example.Booking_Hotel_BE.Dto.Request.OtpRequest;
import com.example.Booking_Hotel_BE.Dto.Response.ApiResponse;
import com.example.Booking_Hotel_BE.Services.OtpService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/otp")
public class OtpController
{
    OtpService otpService;

    // Endpoint tạo OTP từ mail
    @PostMapping("/verifyEmail")
    public ApiResponse<Void> verifyEmail(@RequestBody CreateOtpRequest request)
    {
        otpService.verifyEmail(request.getEmail());
        return ApiResponse.<Void>builder()
                .message("Đã gửi mã OTP về email của bạn.")
                .build();
    }

    // Endpoint xác thực OTP
    @PostMapping("/verifyOtp")
    public ApiResponse<ResponseEntity<String>> verifyOtp(@RequestBody OtpRequest request)
    {
        return ApiResponse.<ResponseEntity<String>>builder()
                .data(otpService.verifyOtp(request))
                .build();
    }

    // Endpoint thay đổi mật khẩu
    @PostMapping("/changePassword")
    public ApiResponse<ResponseEntity<String>> changPassword(@RequestBody ChangePasswordRequest request)
    {
        return ApiResponse.<ResponseEntity<String>>builder()
                .data(otpService.changePassword(request))
                .build();
    }
}
