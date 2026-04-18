package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.CreateItineraryRequest;
import com.example.java_service.Dto.Request.UpdateItineraryRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.ItineraryResponse;
import com.example.java_service.Services.ItineraryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/itinerary")
public class ItineraryController {

    ItineraryService itineraryService;

    @PostMapping
    public ApiResponse<ItineraryResponse> createItinerary(
            @RequestBody CreateItineraryRequest request) {
        return ApiResponse.<ItineraryResponse>builder()
                .message("Tạo lịch trình thành công!")
                .data(itineraryService.createItinerary(request))
                .build();
    }

    @GetMapping
    public ApiResponse<ItineraryResponse> getItinerary(@RequestParam String idItinerary) {
        return ApiResponse.<ItineraryResponse>builder()
                .message("Lấy lịch trình thành công!")
                .data(itineraryService.getItinerary(idItinerary))
                .build();
    }

    @GetMapping("/user")
    public ApiResponse<List<ItineraryResponse>> getUserItineraries(@RequestParam String idUser) {
        return ApiResponse.<List<ItineraryResponse>>builder()
                .message("Lấy danh sách lịch trình thành công!")
                .data(itineraryService.getUserItineraries(idUser))
                .build();
    }

    @PutMapping
    public ApiResponse<ItineraryResponse> updateItinerary(
            @RequestParam String idItinerary,
            @RequestBody UpdateItineraryRequest request) {
        return ApiResponse.<ItineraryResponse>builder()
                .message("Cập nhật lịch trình thành công!")
                .data(itineraryService.updateItinerary(idItinerary, request))
                .build();
    }

    @DeleteMapping
    public ApiResponse<Void> deleteItinerary(@RequestParam String idItinerary) {
        itineraryService.deleteItinerary(idItinerary);
        return ApiResponse.<Void>builder()
                .message("Xóa lịch trình thành công!")
                .build();
    }

    @PatchMapping("/confirm")
    public ApiResponse<Void> confirmItinerary(@RequestParam String idItinerary) {
        itineraryService.confirmItinerary(idItinerary);
        return ApiResponse.<Void>builder()
                .message("Xác nhận lịch trình thành công!")
                .build();
    }
}