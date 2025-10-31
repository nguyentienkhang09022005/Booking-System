package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.HotelCreationRequest;
import com.example.java_service.Dto.Request.HotelUpdateRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.GetDetailHotelResponse;
import com.example.java_service.Dto.Response.GetListHotelResponse;
import com.example.java_service.Services.HotelService;
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
@RequestMapping("/hotel")
public class HotelController
{
    HotelService hotelService;

    // Endpoint tạo khách sạn mới
    @PostMapping("/creation")
    public ApiResponse<ResponseEntity<String>> creationHotel(@RequestBody HotelCreationRequest request)
    {
        var response = hotelService.saveHotel(request);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    // Endpoint lấy danh sách khách sạn đã được phê duyệt
    @GetMapping("/get-list-hotel-approved")
    public ApiResponse<List<GetListHotelResponse>> getHotelsApproved(@RequestParam(required = false) UUID idUser)
    {
        return ApiResponse.<List<GetListHotelResponse>>builder()
                .message("Lấy danh sách khách sạn thành công!")
                .data(hotelService.getListHotelApproved(idUser))
                .build();
    }

    // Endpoint lấy danh sách khách sạn đã được phê duyệt
    @GetMapping("/get-list-hotel-not-approved")
    public ApiResponse<List<GetListHotelResponse>> getHotelsNotApproved()
    {
        return ApiResponse.<List<GetListHotelResponse>>builder()
                .message("Lấy danh sách khách sạn thành công!")
                .data(hotelService.getListHotelNotApproved())
                .build();
    }

    // Endpoint lấy danh sách khách sạn
    @GetMapping("/list-hotel-of-user")
    public ApiResponse<List<GetListHotelResponse>> getHotelsCreatedByUser(@RequestParam UUID idUser)
    {
        return ApiResponse.<List<GetListHotelResponse>>builder()
                .message("Lấy danh sách khách sạn đã tạo thành công!")
                .data(hotelService.getListHotelCreatedByUser(idUser))
                .build();
    }

    // Endpoint lấy thông tin chi tiết khách sạn
    @GetMapping( "/getInfHotel")
    public ApiResponse<GetDetailHotelResponse> getDetailHotel(@RequestParam UUID idHotel,
                                                              @RequestParam(required = false) UUID idUser)
    {
        return ApiResponse.<GetDetailHotelResponse>builder()
                .message("Lấy thông tin chi tiết khách sạn thành công!")
                .data(hotelService.getHotelDetail(idHotel, idUser))
                .build();
    }

    // Endpoint xóa khách sạn
    @DeleteMapping( "/delele")
    public ApiResponse<ResponseEntity<String>> deleteHotel(@RequestParam UUID idHotel)
    {
        var response = hotelService.deleteHotel(idHotel);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    // Endpoint cập nhật khách sạn
    @PatchMapping( "/update")
    public ApiResponse<ResponseEntity<String>> updateHotel(@RequestParam UUID idHotel,
                                                           @RequestBody HotelUpdateRequest request)
    {
        var response = hotelService.updateHotel(idHotel, request);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }
}
