package com.example.java_service.Controllers;

import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Services.ImageService;
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
@RequestMapping("/images")
public class ImageController
{
    ImageService imageService;

    // Endpoint tạo ảnh
    @PostMapping(value = "/creation", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<String>> createImage(@RequestPart(value = "files", required = false) List<MultipartFile> files)
    {
        return ApiResponse.<List<String>>builder()
                .data(imageService.saveImage(files))
                .build();
    }

    // Endpoint xóa ảnh
    @DeleteMapping("/delete")
    public ApiResponse<ResponseEntity<String>> deleteImage(@RequestBody List<String> idImages)
    {
        var response = imageService.deleteImage(idImages);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }
}
