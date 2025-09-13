package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.TypeUserRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.TypeUserResponse;
import com.example.java_service.Models.TypeUser;
import com.example.java_service.Services.TypeUserService;
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
@RequestMapping("/typeuser")
public class TypeUserController
{
    TypeUserService typeUserService;

    // Endpoint lấy danh sách loại người dùng
    @GetMapping( "/list")
    public ApiResponse<List<TypeUser>> getListTypeUser()
    {
        return ApiResponse.<List<TypeUser>>builder()
                .message("Lấy danh sách loại người dùng thành công!")
                .data(typeUserService.getAllTypeUsers())
                .build();
    }

    // Endpoint tạo loại người dùng
    @PostMapping( "/creation")
    public ApiResponse<TypeUserResponse> createTypeUser(@RequestBody TypeUserRequest request)
    {
        return ApiResponse.<TypeUserResponse>builder()
                .message("Tạo loại người dùng thành công!")
                .data(typeUserService.saveTypeUser(request))
                .build();
    }

    // Endpoint xóa loại người dùng
    @DeleteMapping( "/delete")
    public ApiResponse<ResponseEntity<String>> deleteTypeUser(@RequestParam String typeUser)
    {
        var response = typeUserService.deleteTypeUser(typeUser);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }
}
