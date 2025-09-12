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

    // Endpoint sửa ảnh đại diện
    @PutMapping(value = "/updateAvatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ResponseEntity<String>> updateAvatar(@RequestPart(value = "files", required = false) MultipartFile avatar,
                                                            @RequestParam String refId)
    {
        var response = imageService.updateAvatar(avatar, refId);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    // Endpoint sửa ảnh Hotel, Room,...
    @PutMapping(value = "/updateImages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ResponseEntity<List<String>>> updateImages(@RequestPart(value = "files", required = false) List<MultipartFile> images,
                                                                  @RequestParam String refId)
    {
        return ApiResponse.<ResponseEntity<List<String>>>builder()
                .message("Cập nhật hình ảnh thành công!")
                .data(imageService.updateImages(images, refId))
                .build();
    }
}
