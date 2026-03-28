package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.ServiceCreationRequest;
import com.example.java_service.Dto.Request.ServiceUpdateRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.ServiceResponse;
import com.example.java_service.Services.ServiceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/service")
public class ServiceController
{
    ServiceService serviceService;

    // Endpoint lấy danh sách dịch vụ
    @GetMapping("/getListService")
    public ApiResponse<List<ServiceResponse>> getServices(@RequestParam UUID idAccommodation)
    {
        return ApiResponse.<List<ServiceResponse>>builder()
                .message("Lấy danh sách dịch vụ thành công!")
                .data(serviceService.getListService(idAccommodation))
                .build();
    }

    // Endpoint tạo dịch vụ mới
    @PostMapping("/creation")
    public ApiResponse<ServiceResponse> creationService(@RequestBody ServiceCreationRequest request)
    {
        return ApiResponse.<ServiceResponse>builder()
                .message("Tạo dịch vụ thành công!")
                .data(serviceService.saveService(request))
                .build();
    }

    // Endpoint cập nhật dịch vụ
    @PatchMapping( "/update")
    public ApiResponse<ServiceResponse> updateService(@RequestParam UUID idService,
                                                @RequestBody ServiceUpdateRequest request)
    {
        return ApiResponse.<ServiceResponse>builder()
                .message("Thay đổi thông tin dịch vụ thành công!")
                .data(serviceService.updateService(request, idService))
                .build();
    }

    // Endpoint xóa dịch vụ
    @DeleteMapping( "/delele")
    public ApiResponse<ResponseEntity<String>> deleteService(@RequestParam UUID idService)
    {
        var response = serviceService.deleteService(idService);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }
}
