package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.HotelCreationRequest;
import com.example.java_service.Dto.Request.HotelUpdateRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.GetListHotelResponse;
import com.example.java_service.Dto.Response.HotelResponse;
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
    public ApiResponse<HotelResponse> creationHotel(@RequestBody HotelCreationRequest request)
    {
        return ApiResponse.<HotelResponse>builder()
                .message("Tạo khách sạn thành công!")
                .data(hotelService.saveHotel(request))
                .build();
    }

    // Endpoint lấy danh sách khách sạn
    @GetMapping( "/getlist")
    public ApiResponse<List<GetListHotelResponse>> listHotel(@RequestParam UUID idUser)
    {
        return ApiResponse.<List<GetListHotelResponse>>builder()
                .message("Lấy danh sách khách sạn thành công!")
                .data(hotelService.getListHotel(idUser))
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
    public ApiResponse<HotelResponse> updateHotel(@RequestParam UUID idHotel,
                                                           @RequestBody HotelUpdateRequest request)
    {
        return ApiResponse.<HotelResponse>builder()
                .message("Thay đổi thông tin khách sạn thành công!")
                .data(hotelService.updateHotel(idHotel, request))
                .build();
    }
}
