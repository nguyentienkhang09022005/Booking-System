package com.example.Booking_Hotel_BE.Controllers;

import com.example.Booking_Hotel_BE.Dto.Request.OtpRequest;
import com.example.Booking_Hotel_BE.Dto.Request.RegisterRequest;
import com.example.Booking_Hotel_BE.Dto.Response.ApiResponse;
import com.example.Booking_Hotel_BE.Models.Users;
import com.example.Booking_Hotel_BE.Services.OtpService;
import com.example.Booking_Hotel_BE.Services.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/otp")
public class OtpController
{
    OtpService otpService;

    // Endpoint tạo OTP từ mail
    @PostMapping("/verifyEmail/{email}")
    public ApiResponse<Void> verifyEmail(@PathVariable String email)
    {
        otpService.verifyEmail(email);
        return ApiResponse.<Void>builder()
                .message("Đã gửi mã OTP về email của bạn.")
                .build();
    }

    // Endpoint xác thực OTP
    @PostMapping("/verifyOtp")
    public ApiResponse<ResponseEntity<String>> verifyOtp(@RequestBody OtpRequest request)
    {
        otpService.verifyOtp(request);
        return ApiResponse.<ResponseEntity<String>>builder()
                .data(otpService.verifyOtp(request))
                .build();
    }
}
