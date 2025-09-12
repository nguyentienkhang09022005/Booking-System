package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.HotelCreationRequest;
import com.example.java_service.Dto.Request.UserCreationRequest;
import com.example.java_service.Dto.Request.UserUpdateRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Services.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    // Endpoint thay đổi thông tin người dùng
    @PatchMapping("/update")
    public ApiResponse<ResponseEntity<String>> updateUser(@RequestBody UserUpdateRequest request, UUID idUser)
    {
        var response = userService.updateUser(request, idUser);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    // Endpoint xóa người dùng
    @DeleteMapping("/delete")
    public ApiResponse<ResponseEntity<String>> deleteUser(@RequestBody UUID idUser)
    {
        var response = userService.deleteUser(idUser);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

}
