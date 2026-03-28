package com.example.java_service.Controllers;

import com.example.java_service.Dto.Response.*;
import com.example.java_service.Services.BookingFilterService;
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
@RequestMapping("/filter")
public class FilterController {

    BookingFilterService bookingFilterService;

    @GetMapping("/accommodations")
    public ApiResponse<List<SimpleItemResponse>> getSimpleAccommodations() {
        return ApiResponse.<List<SimpleItemResponse>>builder()
                .message("Lấy danh sách nơi cư trú thành công!")
                .data(bookingFilterService.getSimpleAccommodations())
                .build();
    }

    @GetMapping("/tours")
    public ApiResponse<List<SimpleItemResponse>> getSimpleTours() {
        return ApiResponse.<List<SimpleItemResponse>>builder()
                .message("Lấy danh sách tour thành công!")
                .data(bookingFilterService.getSimpleTours())
                .build();
    }

    @GetMapping("/transport-tickets")
    public ApiResponse<List<SimpleItemResponse>> getSimpleTransportTickets() {
        return ApiResponse.<List<SimpleItemResponse>>builder()
                .message("Lấy danh sách vé di chuyển thành công!")
                .data(bookingFilterService.getSimpleTransportTickets())
                .build();
    }

    @GetMapping("/bookings/accommodation")
    public ApiResponse<BookingFilterResponse<BookingListResponse>> filterAccommodationBookings(
            @RequestParam UUID idAccommodation,
            @RequestParam int month,
            @RequestParam int year) {

        return ApiResponse.<BookingFilterResponse<BookingListResponse>>builder()
                .message(String.format("Lọc danh sách đặt phòng tháng %d/%d thành công!", month, year))
                .data(bookingFilterService.filterAccommodationBookings(idAccommodation, month, year))
                .build();
    }

    @GetMapping("/bookings/tour")
    public ApiResponse<BookingFilterResponse<BookingTourResponse>> filterTourBookings(
            @RequestParam UUID idTour,
            @RequestParam int month,
            @RequestParam int year) {

        return ApiResponse.<BookingFilterResponse<BookingTourResponse>>builder()
                .message(String.format("Lọc danh sách đặt tour tháng %d/%d thành công!", month, year))
                .data(bookingFilterService.filterTourBookings(idTour, month, year))
                .build();
    }

    @GetMapping("/bookings/transport")
    public ApiResponse<BookingFilterResponse<BookingTransportResponse>> filterTransportBookings(
            @RequestParam UUID idTicket,
            @RequestParam int month,
            @RequestParam int year) {

        return ApiResponse.<BookingFilterResponse<BookingTransportResponse>>builder()
                .message(String.format("Lọc danh sách đặt vé di chuyển tháng %d/%d thành công!", month, year))
                .data(bookingFilterService.filterTransportBookings(idTicket, month, year))
                .build();
    }
}