package com.example.Booking_Hotel_BE.Controllers;

import com.example.Booking_Hotel_BE.Dto.Request.RegisterRequest;
import com.example.Booking_Hotel_BE.Dto.Response.ApiResponse;
import com.example.Booking_Hotel_BE.Services.RegisterService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/register")
public class RegisterController
{
    RegisterService registerService;

    // Endpoint đăng ký
    @PostMapping
    public ApiResponse<Void> Register(@RequestBody RegisterRequest request)
    {
        registerService.register(request);
        return ApiResponse.<Void>builder()
                .message("Successful registration")
                .build();
    }
}
