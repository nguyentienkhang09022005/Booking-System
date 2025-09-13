package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.RoleRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.RoleResponse;
import com.example.java_service.Models.Roles;
import com.example.java_service.Services.RoleService;
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
@RequestMapping("/roles")
public class RoleController
{
    RoleService roleService;

    // Endpoint lấy danh sách quyền người dùng
    @GetMapping( "/list")
    public ApiResponse<List<Roles>> getListRoles()
    {
        return ApiResponse.<List<Roles>>builder()
                .message("Lấy danh sách quyền người dùng thành công!")
                .data(roleService.getAllRoles())
                .build();
    }

    // Endpoint tạo quyền người dùng
    @PostMapping( "/creation")
    public ApiResponse<RoleResponse> createRole(@RequestBody RoleRequest request)
    {
        return ApiResponse.<RoleResponse>builder()
                .message("Tạo quyền người dùng thành công!")
                .data(roleService.saveRole(request))
                .build();
    }

    // Endpoint xóa quyền người dùng
    @DeleteMapping( "/delete")
    public ApiResponse<ResponseEntity<String>> deleteRole(@RequestParam String role)
    {
        var response = roleService.deleteRole(role);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }
}
