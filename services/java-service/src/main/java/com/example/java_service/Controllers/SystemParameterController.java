package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.SystemParameterUpdateRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.SystemParameterResponse;
import com.example.java_service.Services.SystemParameterService;
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
@RequestMapping("/system-parameter")
public class SystemParameterController {

    SystemParameterService systemParameterService;

    // Endpoint lấy danh sách tham số hệ thống
    @GetMapping("/list-system-parameter")
    public ApiResponse<List<SystemParameterResponse>> getAllSystemParameters() {
        return ApiResponse.<List<SystemParameterResponse>>builder()
                .message("Lấy danh sách tham số hệ thống thành công!")
                .data(systemParameterService.getAllSystemParameters())
                .build();
    }

    // Endpoint cập nhật tham số hệ thống
    @PatchMapping("/update")
    public ApiResponse<SystemParameterResponse> updateSystemParameter(@RequestParam String paramKey,
                                                                      @RequestBody SystemParameterUpdateRequest request) {
        return ApiResponse.<SystemParameterResponse>builder()
                .message("Thay đổi thông tin tham số hệ thống thành công!")
                .data(systemParameterService.updateSystemParameter(paramKey, request))
                .build();
    }
}