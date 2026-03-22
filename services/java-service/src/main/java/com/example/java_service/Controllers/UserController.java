package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.UserCreationRequest;
import com.example.java_service.Dto.Request.UserUpdateRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.UserResponse;
import com.example.java_service.Services.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
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
    public ApiResponse<UserResponse> createUser(@RequestBody UserCreationRequest request)
    {
        return ApiResponse.<UserResponse>builder()
                .message("Tạo người dùng thành công!")
                .data(userService.saveUser(request))
                .build();
    }

    // Endpoint thay đổi thông tin người dùng
    @PatchMapping("/update")
    public ApiResponse<UserResponse> updateUser(@RequestBody UserUpdateRequest request,
                                                @RequestParam UUID idUser)
    {
        return ApiResponse.<UserResponse>builder()
                .message("Thay đổi thông tin người dùng thành công!")
                .data(userService.updateUser(request, idUser))
                .build();
    }

    // Endpoint xóa người dùng
    @DeleteMapping("/delete")
    public ApiResponse<ResponseEntity<String>> deleteUser(@RequestParam UUID idUser)
    {
        var response = userService.deleteUser(idUser);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    // Endpoint lấy danh sách người dùng
    @GetMapping("/getListUser")
    public ApiResponse<List<UserResponse>> getListUser()
    {
        return ApiResponse.<List<UserResponse>>builder()
                .message("Lấy danh sách người dùng thành công!")
                .data(userService.getListUsers())
                .build();
    }

    // Endpoint lấy thông tin người dùng
    @GetMapping("/getInfUser")
    public ApiResponse<UserResponse> getListUser(@RequestParam UUID idUser)
    {
        return ApiResponse.<UserResponse>builder()
                .message("Lấy thông tin chi tiết người dùng thành công!")
                .data(userService.getInfUser(idUser))
                .build();
    }

    // Internal Enpoint
    @PostMapping("/add-loyalty-points/{idUser}")
    public ResponseEntity<String> addLoyaltyPoints(@PathVariable UUID idUser,
                                                   @RequestParam BigDecimal amount) {

        userService.addLoyaltyPoints(idUser, amount);
        return ResponseEntity.ok("Cộng điểm thành công!");
    }
}
