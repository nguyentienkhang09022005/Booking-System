package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.HotelCreationRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.HotelCreationResponse;
import com.example.java_service.Services.HotelService;
import lombok.AccessLevel;
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

    // Endpoint tạo OTP cho quên mật khẩu
    @PostMapping(value = "/creation", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ResponseEntity<String>> creationHotel(@RequestPart("request") HotelCreationRequest request,
                                                     @RequestPart(value = "files", required = false) List<MultipartFile> files)
    {
        request.setListImageHotel(files);
        var response = hotelService.saveHotel(request);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }
}
