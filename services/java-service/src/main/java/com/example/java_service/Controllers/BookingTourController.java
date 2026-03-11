package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.TourApproveRequest;
import com.example.java_service.Dto.Request.TourCreationRequest;
import com.example.java_service.Dto.Request.TourUpdateRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.TourResponse;
import com.example.java_service.Services.TourService;
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
@RequestMapping("/tour")
public class TourController
{
    TourService tourService;

    // Lấy tất cả tour
    @GetMapping("/get-all")
    public ApiResponse<List<TourResponse>> getAllTours()
    {
        return ApiResponse.<List<TourResponse>>builder()
                .message("Lấy danh sách tất cả tour thành công!")
                .data(tourService.getAllTours())
                .build();
    }

    // Lấy danh sách tour đã duyệt
    @GetMapping("/get-list-approved")
    public ApiResponse<List<TourResponse>> getToursApproved()
    {
        return ApiResponse.<List<TourResponse>>builder()
                .message("Lấy danh sách tour đã được phê duyệt thành công!")
                .data(tourService.getListTourApproved())
                .build();
    }

    // Lấy danh sách tour chưa duyệt
    @GetMapping("/get-list-not-approved")
    public ApiResponse<List<TourResponse>> getToursNotApproved() {
        return ApiResponse.<List<TourResponse>>builder()
                .message("Lấy danh sách tour chưa duyệt thành công!")
                .data(tourService.getListTourNotApproved())
                .build();
    }

    // Lấy danh sách tour của user
    @GetMapping("/list-of-user")
    public ApiResponse<List<TourResponse>> getToursCreatedByUser(@RequestParam UUID idUser)
    {
        return ApiResponse.<List<TourResponse>>builder()
                .message("Lấy danh sách tour của người dùng thành công!")
                .data(tourService.getListTourCreatedByUser(idUser))
                .build();
    }

    // Thêm tour mới
    @PostMapping("/creation")
    public ApiResponse<ResponseEntity<String>> creationTour(@RequestBody TourCreationRequest request)
    {
        var response = tourService.saveTour(request);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    // Sửa thông tin tour
    @PatchMapping("/update")
    public ApiResponse<ResponseEntity<String>> updateTour(@RequestParam UUID idTour,
                                                          @RequestBody TourUpdateRequest request)
    {
        var response = tourService.updateTour(idTour, request);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    // Xóa tour
    @DeleteMapping("/delete")
    public ApiResponse<ResponseEntity<String>> deleteTour(@RequestParam UUID idTour)
    {
        var response = tourService.deleteTour(idTour);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    // Phê duyệt tour
    @PatchMapping("/approve")
    public ApiResponse<ResponseEntity<String>> approveTour(@RequestParam UUID idTour,
                                                           @RequestBody TourApproveRequest request)
    {
        var response = tourService.approveTour(idTour, request);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }
}