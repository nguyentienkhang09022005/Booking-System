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

    // Endpoint tạo OTP cho quên mật khẩu
    @PostMapping("/forgot_password")
    public ApiResponse<Void> sendForgotPasswordOtp(@RequestBody CreateOtpRequest request)
    {
        otpService.sendForgotPasswordOtp(request.getEmail());
        return ApiResponse.<Void>builder()
                .message("Đã gửi mã OTP về email của bạn.")
                .build();
    }

    @PostMapping("/register")
    public ApiResponse<Void> sendRegisterOtp(@RequestBody CreateOtpRequest request)
    {
        otpService.sendRegisterOtp(request.getEmail());
        return ApiResponse.<Void>builder()
                .message("Đã gửi mã OTP về email của bạn.")
                .build();
    }

    // Endpoint xác thực OTP cho quên mật khẩu
    @PostMapping("/verifyOtpForgotPassword")
    public ApiResponse<ResponseEntity<String>> verifyForgotPasswordOtp(@RequestBody OtpRequest request)
    {
        return ApiResponse.<ResponseEntity<String>>builder()
                .data(otpService.verifyForgotPasswordOtp(request))
                .build();
    }

    // Endpoint xác thực OTP cho đăng ký
    @PostMapping("/verifyOtpRegister")
    public ApiResponse<ResponseEntity<String>> verifyRegisterOtp(@RequestBody OtpRequest request)
    {
        return ApiResponse.<ResponseEntity<String>>builder()
                .data(otpService.verifyRegisterOtp(request))
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
