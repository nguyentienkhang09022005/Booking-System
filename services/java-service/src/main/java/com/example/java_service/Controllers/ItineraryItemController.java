package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.CreateItineraryItemRequest;
import com.example.java_service.Dto.Request.ReorderItemsRequest;
import com.example.java_service.Dto.Request.UpdateItineraryItemRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.ItineraryItemResponse;
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
@RequestMapping("/itinerary/items")
public class ItineraryItemController {

    ItineraryService itineraryService;

    @GetMapping("/list")
    public ApiResponse<List<ItineraryItemResponse>> getItineraryItems(
            @RequestParam String idItinerary) {
        return ApiResponse.<List<ItineraryItemResponse>>builder()
                .message("Lấy danh sách hạng mục thành công!")
                .data(itineraryService.getItineraryItems(idItinerary))
                .build();
    }

    @GetMapping("/inf")
    public ApiResponse<ItineraryItemResponse> getItineraryItem(
            @RequestParam String idItem) {
        return ApiResponse.<ItineraryItemResponse>builder()
                .message("Lấy hạng mục thành công!")
                .data(itineraryService.getItineraryItem(idItem))
                .build();
    }

    @PostMapping
    public ApiResponse<ItineraryItemResponse> createItineraryItem(
            @RequestParam String idItinerary,
            @RequestBody CreateItineraryItemRequest request) {
        return ApiResponse.<ItineraryItemResponse>builder()
                .message("Tạo hạng mục thành công!")
                .data(itineraryService.createItineraryItem(idItinerary, request))
                .build();
    }

    @PutMapping
    public ApiResponse<ItineraryItemResponse> updateItineraryItem(
            @RequestParam String idItem,
            @RequestBody UpdateItineraryItemRequest request) {
        return ApiResponse.<ItineraryItemResponse>builder()
                .message("Cập nhật hạng mục thành công!")
                .data(itineraryService.updateItineraryItem(idItem, request))
                .build();
    }

    @PatchMapping("/status")
    public ApiResponse<ItineraryItemResponse> updateItemStatus(
            @RequestParam String idItem,
            @RequestBody UpdateItineraryItemRequest request) {
        return ApiResponse.<ItineraryItemResponse>builder()
                .message("Cập nhật trạng thái hạng mục thành công!")
                .data(itineraryService.updateItemStatus(idItem, request))
                .build();
    }

    @PatchMapping("/reorder")
    public ApiResponse<Void> reorderItems(
            @RequestParam String idItinerary,
            @RequestBody ReorderItemsRequest request) {
        itineraryService.reorderItems(idItinerary, request);
        return ApiResponse.<Void>builder()
                .message("Sắp xếp lại thứ tự hạng mục thành công!")
                .build();
    }

    @DeleteMapping
    public ApiResponse<Void> deleteItineraryItem(@RequestParam String idItem) {
        itineraryService.deleteItineraryItem(idItem);
        return ApiResponse.<Void>builder()
                .message("Xóa hạng mục thành công!")
                .build();
    }
}