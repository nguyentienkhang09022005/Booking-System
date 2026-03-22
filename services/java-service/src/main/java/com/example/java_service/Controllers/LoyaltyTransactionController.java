package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.AddPointRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.LoyaltyTransactionResponse;
import com.example.java_service.Services.LoyaltyTransactionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/loyalty")
public class LoyaltyTransactionController {

    LoyaltyTransactionService loyaltyTransactionService;

    @PostMapping("/add-points")
    public ResponseEntity<String> addLoyaltyPoints(@RequestBody AddPointRequest request) {
        loyaltyTransactionService.processLoyaltyPoints(request);
        return ResponseEntity.ok("Xử lý điểm thành công!");
    }

    @GetMapping("/my-history")
    public ApiResponse<List<LoyaltyTransactionResponse>> getMyHistory(@RequestParam UUID idUser) {
        return ApiResponse.<List<LoyaltyTransactionResponse>>builder()
                .message("Lấy tất cả lịch sử tích điểm thành công!")
                .data(loyaltyTransactionService.getHistoryByUser(idUser))
                .build();
    }

    @GetMapping("/booking-room")
    public ApiResponse<List<LoyaltyTransactionResponse>> getByBookingRoom(@RequestParam UUID idUser) {
        return ApiResponse.<List<LoyaltyTransactionResponse>>builder()
                .message("Lấy lịch sử tích điểm của đơn đặt phòng thành công!")
                .data(loyaltyTransactionService.getRoomBookingHistoryByUser(idUser))
                .build();
    }

    @GetMapping("/booking-tour")
    public ApiResponse<List<LoyaltyTransactionResponse>> getByBookingTour(@RequestParam UUID idUser) {
        return ApiResponse.<List<LoyaltyTransactionResponse>>builder()
                .message("Lấy lịch sử tích điểm của đơn đặt tour thành công!")
                .data(loyaltyTransactionService.getTourBookingHistoryByUser(idUser))
                .build();
    }

    @GetMapping("/booking-transport")
    public ApiResponse<List<LoyaltyTransactionResponse>> getByBookingTransport(@RequestParam UUID idUser) {
        return ApiResponse.<List<LoyaltyTransactionResponse>>builder()
                .message("Lấy lịch sử tích điểm của đơn đặt vé di chuyển thành công!")
                .data(loyaltyTransactionService.getTransportBookingHistoryByUser(idUser))
                .build();
    }
}