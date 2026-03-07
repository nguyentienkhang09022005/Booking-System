package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.AccommodationCreationRequest;
import com.example.java_service.Dto.Request.AccommodationUpdateRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.GetDetailAccommodationResponse;
import com.example.java_service.Dto.Response.GetListAccommodationResponse;
import com.example.java_service.Services.AccommodationService;
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
@RequestMapping("/accommodation")
public class AccommodationController
{
    AccommodationService accommodationService;

    // Endpoint tạo nơi cư trú mới
    @PostMapping("/creation")
    public ApiResponse<ResponseEntity<String>> creationAccommodation(@RequestBody AccommodationCreationRequest request)
    {
        var response = accommodationService.saveAccommodation(request);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    // Endpoint lấy danh sách nơi cư trú đã được phê duyệt
    @GetMapping("/get-list-accommodation-approved")
    public ApiResponse<List<GetListAccommodationResponse>> getAccommodationsApproved(@RequestParam(required = false) UUID idUser)
    {
        return ApiResponse.<List<GetListAccommodationResponse>>builder()
                .message("Lấy danh sách nơi cư trú thành công!")
                .data(accommodationService.getListAccommodationApproved(idUser))
                .build();
    }

    // Endpoint lấy danh sách nơi cư trú đã được phê duyệt
    @GetMapping("/get-list-accommodation-not-approved")
    public ApiResponse<List<GetListAccommodationResponse>> getAccommodationsNotApproved()
    {
        return ApiResponse.<List<GetListAccommodationResponse>>builder()
                .message("Lấy danh sách nơi cư trú thành công!")
                .data(accommodationService.getListAccommodationNotApproved())
                .build();
    }

    // Endpoint lấy danh sách nơi cư trú
    @GetMapping("/list-accommodation-of-user")
    public ApiResponse<List<GetListAccommodationResponse>> getAccommodationsCreatedByUser(@RequestParam UUID idUser)
    {
        return ApiResponse.<List<GetListAccommodationResponse>>builder()
                .message("Lấy danh sách nơi cư tr đã tạo thành công!")
                .data(accommodationService.getListAccommodationCreatedByUser(idUser))
                .build();
    }

    // Endpoint lấy thông tin chi tiết nơi cư trú
    @GetMapping( "/getInfAccommodation")
    public ApiResponse<GetDetailAccommodationResponse> getDetailAccommodation(@RequestParam UUID idAccommodation,
                                                                      @RequestParam(required = false) UUID idUser)
    {
        return ApiResponse.<GetDetailAccommodationResponse>builder()
                .message("Lấy thông tin chi tiết nơi cư trú thành công!")
                .data(accommodationService.getAccommodationDetail(idAccommodation, idUser))
                .build();
    }

    // Endpoint xóa nơi cư trú
    @DeleteMapping( "/delele")
    public ApiResponse<ResponseEntity<String>> deleteAccommodation(@RequestParam UUID idAccommodation)
    {
        var response = accommodationService.deleteAccommodation(idAccommodation);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    // Endpoint cập nhật nơi cư trú
    @PatchMapping( "/update")
    public ApiResponse<ResponseEntity<String>> updateAccommodation(@RequestParam UUID idAccommodation,
                                                           @RequestBody AccommodationUpdateRequest request)
    {
        var response = accommodationService.updateAccommodation(idAccommodation, request);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }
}
