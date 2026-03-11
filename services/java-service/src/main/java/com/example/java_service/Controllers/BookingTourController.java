package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.BookingTourCreationRequest;
import com.example.java_service.Dto.Request.BookingTourUpdateRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.BookingTourResponse;
import com.example.java_service.Dto.Response.PaymentResponse;
import com.example.java_service.Services.BookingTourService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/booking-tour")
public class BookingTourController
{
    BookingTourService bookingTourService;

    @PostMapping("/creation")
    public ApiResponse<PaymentResponse> createBookingTour(@RequestBody BookingTourCreationRequest request,
                                                          @RequestParam String paymentMethod,
                                                          @RequestParam(required = false) String payerAddress)
    {
        return ApiResponse.<PaymentResponse>builder()
                .message("Tạo đơn đặt tour và khởi tạo thanh toán thành công!")
                .data(bookingTourService.createBookingTour(request, paymentMethod, payerAddress))
                .build();
    }

    @GetMapping("/detail")
    public ApiResponse<BookingTourResponse> getBookingTourDetail(@RequestParam UUID idBookingTour)
    {
        return ApiResponse.<BookingTourResponse>builder()
                .message("Lấy chi tiết đơn đặt tour thành công!")
                .data(bookingTourService.getBookingTourDetail(idBookingTour))
                .build();
    }

    @GetMapping("/list-of-user")
    public ApiResponse<List<BookingTourResponse>> getListBookingTourByUser(@RequestParam UUID idUser)
    {
        return ApiResponse.<List<BookingTourResponse>>builder()
                .message("Lấy danh sách đơn đặt tour của người dùng thành công!")
                .data(bookingTourService.getListBookingTourByUser(idUser))
                .build();
    }

    @GetMapping("/list-of-business")
    public ApiResponse<List<BookingTourResponse>> getListBookingTourByBusiness(@RequestParam UUID idUser)
    {
        return ApiResponse.<List<BookingTourResponse>>builder()
                .message("Lấy danh sách đơn đặt tour của doanh nghiệp thành công!")
                .data(bookingTourService.getListBookingTourByBusiness(idUser))
                .build();
    }

    @PatchMapping("/update")
    public ApiResponse<BookingTourResponse> updateBookingTour(@RequestParam UUID idBookingTour,
                                                              @RequestBody BookingTourUpdateRequest request)
    {
        return ApiResponse.<BookingTourResponse>builder()
                .message("Cập nhật thông tin đơn đặt tour thành công!")
                .data(bookingTourService.updateBookingTour(idBookingTour, request))
                .build();
    }
}