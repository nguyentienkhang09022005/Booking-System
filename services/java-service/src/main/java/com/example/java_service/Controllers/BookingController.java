package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.BookingRequest;
import com.example.java_service.Dto.Request.BookingUpdateRequest;
import com.example.java_service.Dto.Request.BookingUpdateStatusRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.BookingListResponse;
import com.example.java_service.Dto.Response.BookingResponse;
import com.example.java_service.Dto.Response.PaymentResponse;
import com.example.java_service.Services.BookingHandleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/booking")
public class BookingController
{
    BookingHandleService bookingHandleService;

    // Endpoint lấy danh sách booking theo khách sạn
    @GetMapping("/getListBooking")
    public ApiResponse<List<BookingListResponse>> getListBooking(@RequestParam UUID idHotel)
    {
        return ApiResponse.<List<BookingListResponse>>builder()
                .message("Lấy danh sách đặt phòng của khách sạn thành công!")
                .data(bookingHandleService.getListBooking(idHotel))
                .build();
    }

    // Endpoint lấy lịch sử booking của người dùng
    @GetMapping("/getHistoryListBooking")
    public ApiResponse<List<BookingListResponse>> getHistoryListBooking(@RequestParam UUID idUser)
    {
        return ApiResponse.<List<BookingListResponse>>builder()
                .message("Lấy lịch sử đặt phòng của người dùng thành công!")
                .data(bookingHandleService.getHistoryListBooking(idUser))
                .build();
    }

    // Endpoint lấy thông tin booking theo khách sạn
    @GetMapping("/getInfBooking")
    public ApiResponse<BookingResponse> getInfBooking(@RequestParam UUID idBooking)
    {
        return ApiResponse.<BookingResponse>builder()
                .message("Lấy thông tin đặt phòng thành công!")
                .data(bookingHandleService.getInfBooking(idBooking))
                .build();
    }

    // Endpoint tạo đặt phòng mới
    @PostMapping("/creation")
    public ApiResponse<PaymentResponse> creationBooking(@RequestBody BookingRequest request)
    {
        var response = bookingHandleService.saveBooking(request);
        return ApiResponse.<PaymentResponse>builder()
                .message("Đặt phòng thành công vui lòng thanh toán!")
                .data(response)
                .build();
    }

    // Endpoint cập nhật thông tin booking
    @PatchMapping("/update-information")
    public ApiResponse<ResponseEntity<String>> updateBooking(@RequestParam UUID idBooking,
                                                             @RequestBody BookingUpdateRequest request)
    {
        var response = bookingHandleService.updateBooking(request, idBooking);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    // Endpoint cập nhật trạng thái booking
    @PatchMapping("/update-status")
    public ApiResponse<ResponseEntity<String>> updateStatusBooking(@RequestParam UUID idBooking,
                                                                       @RequestBody BookingUpdateStatusRequest request)
    {
        var response = bookingHandleService.updateStatusBooking(request, idBooking);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    // Endpoint hủy booking
    @PatchMapping("/cancel-booking")
    public ApiResponse<ResponseEntity<String>> cancelBooking(@RequestParam UUID idBooking)
    {
        var response = bookingHandleService.cancelBookingWhenStatusPending(idBooking);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    // Endpoint xóa booking
    @DeleteMapping("/delete")
    public ApiResponse<ResponseEntity<String>> deleteBooking(@RequestParam UUID idBooking)
    {
        var response = bookingHandleService.deleteBookingRoom(idBooking);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }
}
