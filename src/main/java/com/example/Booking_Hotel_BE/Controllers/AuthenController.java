package com.example.Booking_Hotel_BE.Controllers;

import com.example.Booking_Hotel_BE.Dto.Request.AuthenRequest;
import com.example.Booking_Hotel_BE.Dto.Request.LogoutRequest;
import com.example.Booking_Hotel_BE.Dto.Response.ApiResponse;
import com.example.Booking_Hotel_BE.Dto.Response.AuthenResponse;
import com.example.Booking_Hotel_BE.Services.AuthenService;
import com.nimbusds.jose.JOSEException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/authen")
public class AuthenController
{
    final AuthenService authenService;

    // Endpoint đăng nhập tạo TOKEN
    @PostMapping("/login")
    public ApiResponse<AuthenResponse> authenticate(@RequestBody AuthenRequest request) {
        var result = authenService.authenticated(request);
        return ApiResponse.<AuthenResponse>builder()

                .data(result)
                .build();
    }

    // Endpoint đăng xuất TOKEN
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody LogoutRequest request) throws ParseException, JOSEException {
        authenService.Logout(request);
        return ApiResponse.<Void>builder()
                .build();
    }

}
