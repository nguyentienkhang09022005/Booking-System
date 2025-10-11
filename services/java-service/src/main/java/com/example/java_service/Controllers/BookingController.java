package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.BookingRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.PaymentResponse;
import com.example.java_service.Services.BookingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/booking")
public class BookingController
{
    BookingService bookingService;

    // Endpoint tạo đặt phòng mới
    @PostMapping("/creation")
    public ApiResponse<PaymentResponse> creationBooking(@RequestBody BookingRequest request)
    {
        var response = bookingService.saveBooking(request);
        return ApiResponse.<PaymentResponse>builder()
                .message("Đặt phòng thành công vui lòng thanh toán!")
                .data(response)
                .build();
    }
}
