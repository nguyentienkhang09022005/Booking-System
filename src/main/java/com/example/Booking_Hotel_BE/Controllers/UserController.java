package com.example.Booking_Hotel_BE.Controllers;

import com.example.Booking_Hotel_BE.Dto.Request.RegisterRequest;
import com.example.Booking_Hotel_BE.Dto.Response.ApiResponse;
import com.example.Booking_Hotel_BE.Models.Users;
import com.example.Booking_Hotel_BE.Services.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/users")
public class UserController
{
    UserService userService;

    // Endpoint lấy danh sách người dùng
    @GetMapping
    public List<Users> findAll()
    {
        return userService.findAll();
    }

    // Endpoint đăng ký
    @PostMapping("/register")
    public ApiResponse<Void> Register(@RequestBody RegisterRequest request)
    {
        userService.register(request);
        return ApiResponse.<Void>builder()
                .message("Successful registration")
                .build();
    }
}
