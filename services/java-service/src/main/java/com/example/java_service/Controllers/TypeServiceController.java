package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.TypeServiceRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.TypeServiceResponse;
import com.example.java_service.Models.TypeService;
import com.example.java_service.Services.TypeServiceService;
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
@RequestMapping("/typeService")
public class TypeServiceController
{
    TypeServiceService typeServiceService;

    // Endpoint lấy danh sách loại dịch vụ
    @GetMapping( "/list")
    public ApiResponse<List<TypeService>> getListTypeService()
    {
        return ApiResponse.<List<TypeService>>builder()
                .message("Lấy danh sách loại dịch vụ thành công!")
                .data(typeServiceService.getAllTypeServices())
                .build();
    }

    // Endpoint tạo loại dịch vụ
    @PostMapping( "/creation")
    public ApiResponse<TypeServiceResponse> createTypeService(@RequestBody TypeServiceRequest request)
    {
        return ApiResponse.<TypeServiceResponse>builder()
                .message("Tạo loại dịch vụ thành công!")
                .data(typeServiceService.saveTypeService(request))
                .build();
    }

    // Endpoint xóa loại dịch vụ
    @DeleteMapping( "/delete")
    public ApiResponse<ResponseEntity<String>> deleteTypeService(@RequestParam String typeService)
    {
        var response = typeServiceService.deleteTypeService(typeService);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }
}
