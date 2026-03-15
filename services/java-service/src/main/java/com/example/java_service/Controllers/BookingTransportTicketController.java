package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.BookingTransportCreationRequest;
import com.example.java_service.Dto.Request.BookingTransportUpdateRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.BookingTransportResponse;
import com.example.java_service.Dto.Response.PaymentResponse;
import com.example.java_service.Services.BookingTransportTicketService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/booking-transport")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingTransportTicketController {

    BookingTransportTicketService bookingService;

    @PostMapping("/creation")
    public ApiResponse<PaymentResponse> createBookingTransport(@RequestBody BookingTransportCreationRequest request,
                                                               @RequestParam(defaultValue = "ZaloPay") String paymentMethod,
                                                               @RequestParam(required = false) String payerAddress)
    {
        return ApiResponse.<PaymentResponse>builder()
                .message("Tạo đơn đặt vé và khởi tạo thanh toán thành công!")
                .data(bookingService.createBookingTransport(request, paymentMethod, payerAddress))
                .build();
    }

    @GetMapping("/detail")
    public ApiResponse<BookingTransportResponse> getBookingDetail(@RequestParam UUID idBookingTransport)
    {
        return ApiResponse.<BookingTransportResponse>builder()
                .message("Lấy thông tin chi tiết đơn đặt vé thành công!")
                .data(bookingService.getBookingDetail(idBookingTransport))
                .build();
    }

    @GetMapping("/list-of-user")
    public ApiResponse<List<BookingTransportResponse>> getListBookingByUser(@RequestParam UUID idUser)
    {
        return ApiResponse.<List<BookingTransportResponse>>builder()
                .message("Lấy danh sách đơn đặt vé của người dùng thành công!")
                .data(bookingService.getListBookingByUser(idUser))
                .build();
    }

    @GetMapping("/list-of-business")
    public ApiResponse<List<BookingTransportResponse>> getListBookingByBusiness(@RequestParam UUID idUser)
    {
        return ApiResponse.<List<BookingTransportResponse>>builder()
                .message("Lấy danh sách đơn đặt vé cho doanh nghiệp thành công!")
                .data(bookingService.getListBookingByBusiness(idUser))
                .build();
    }

    @PatchMapping("/update")
    public ApiResponse<PaymentResponse> updateBookingTransport(@RequestParam UUID idBookingTransport,
                                                               @RequestBody BookingTransportUpdateRequest request)
    {
        return ApiResponse.<PaymentResponse>builder()
                .message("Cập nhật đơn đặt vé và tạo lại thanh toán thành công!")
                .data(bookingService.updateBookingTransport(idBookingTransport, request))
                .build();
    }

    @DeleteMapping("/cancel")
    public ApiResponse<String> cancelBookingTransport(@RequestParam UUID idBookingTransport)
    {
        return ApiResponse.<String>builder()
                .message(bookingService.cancelBookingTransport(idBookingTransport))
                .build();
    }
}