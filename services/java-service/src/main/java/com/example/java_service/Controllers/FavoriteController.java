package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.FavoriteRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Services.FavoriteService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/favorite")
public class FavoriteController
{
    FavoriteService favoriteService;

    // Endpoint tạo yêu thích
    @PostMapping( "/creation")
    public ApiResponse<ResponseEntity<String>> createFavorite(@RequestBody FavoriteRequest request)
    {
        var response = favoriteService.saveFavorite(request);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    // Endpoint xóa yêu thích
    @DeleteMapping( "/delete")
    public ApiResponse<ResponseEntity<String>> deleteFavorite(@RequestParam FavoriteRequest request)
    {
        var response = favoriteService.deleteFavorite(request);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }
}
