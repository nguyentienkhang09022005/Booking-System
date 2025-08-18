package com.example.Booking_Hotel_BE.Controllers;

import com.example.Booking_Hotel_BE.Models.Users;
import com.example.Booking_Hotel_BE.Services.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
