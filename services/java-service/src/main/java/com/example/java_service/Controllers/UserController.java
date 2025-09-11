package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.HotelCreationRequest;
import com.example.java_service.Dto.Request.UserCreationRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Services.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/users")
public class UserController
{
    UserService userService;

    // Endpoint tạo người dùng mới
    @PostMapping("/creation")
    public ApiResponse<ResponseEntity<String>> createUser(@RequestBody UserCreationRequest request)
    {
        var response = userService.saveUser(request);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

}
