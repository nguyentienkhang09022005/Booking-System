package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.UserPreferencesRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.UserPreferencesResponse;
import com.example.java_service.Services.UserPreferencesService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user-preferences")
public class UserPreferencesController {

    UserPreferencesService userPreferencesService;

    @PostMapping
    public ApiResponse<UserPreferencesResponse> createOrUpdate(
            @RequestBody UserPreferencesRequest request) {
        return ApiResponse.<UserPreferencesResponse>builder()
                .message("Lưu sở thích thành công!")
                .data(userPreferencesService.createOrUpdate(request.getIdUser(), request))
                .build();
    }

    @GetMapping("")
    public ApiResponse<UserPreferencesResponse> getPreferences(@RequestParam String idUser) {
        return ApiResponse.<UserPreferencesResponse>builder()
                .message("Lấy sở thích thành công!")
                .data(userPreferencesService.getPreferences(idUser))
                .build();
    }

    @GetMapping("/or-default")
    public ApiResponse<UserPreferencesResponse> getPreferencesOrDefault(@RequestParam String idUser) {
        return ApiResponse.<UserPreferencesResponse>builder()
                .message("Lấy sở thích thành công!")
                .data(userPreferencesService.getPreferencesOrDefault(idUser))
                .build();
    }

    @GetMapping("/onboarding-status")
    public ApiResponse<Boolean> getOnboardingStatus(@RequestParam String idUser) {
        return ApiResponse.<Boolean>builder()
                .message("Lấy trạng thái onboarding thành công!")
                .data(userPreferencesService.hasCompletedOnboarding(idUser))
                .build();
    }
}