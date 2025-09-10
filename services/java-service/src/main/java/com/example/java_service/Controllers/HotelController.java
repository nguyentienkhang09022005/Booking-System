package com.example.java_service.Controllers;

import com.cloudinary.Api;
import com.example.java_service.Dto.Request.GetListHotelRequest;
import com.example.java_service.Dto.Request.HotelCreationRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.GetListHotelResponse;
import com.example.java_service.Dto.Response.HotelCreationResponse;
import com.example.java_service.Services.HotelService;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    // Endpoint lấy danh sách khách sạn
    @GetMapping( "/getlist")
    public ApiResponse<List<GetListHotelResponse>> listHotel(@RequestBody GetListHotelRequest request)
    {
        return ApiResponse.<List<GetListHotelResponse>>builder()
                .message("Lấy danh sách khách sạn thành công!")
                .data(hotelService.getListHotel(request))
                .build();
    }
}
