package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.AccommodationSearchRequest;
import com.example.java_service.Dto.Request.TourSearchRequest;
import com.example.java_service.Dto.Request.TransportTicketSearchRequest;
import com.example.java_service.Dto.Request.VoucherSearchRequest;
import com.example.java_service.Dto.Response.*;
import com.example.java_service.Services.ItemSearchService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/item-search")
public class ItemSearchController {

    private final ItemSearchService itemSearchService;

    @GetMapping("/accommodations")
    public ApiResponse<FilterPageResponse<AccommodationFilterResponse>> searchAccommodations(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "relevance") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        AccommodationSearchRequest request = AccommodationSearchRequest.builder()
                .keyword(keyword)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();

        return ApiResponse.<FilterPageResponse<AccommodationFilterResponse>>builder()
                .message("Tìm kiếm nơi cư trú thành công!")
                .data(itemSearchService.searchAccommodations(request))
                .build();
    }

    @GetMapping("/tours")
    public ApiResponse<FilterPageResponse<TourFilterResponse>> searchTours(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "relevance") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        TourSearchRequest request = TourSearchRequest.builder()
                .keyword(keyword)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();

        return ApiResponse.<FilterPageResponse<TourFilterResponse>>builder()
                .message("Tìm kiếm tour du lịch thành công!")
                .data(itemSearchService.searchTours(request))
                .build();
    }

    @GetMapping("/transport-tickets")
    public ApiResponse<FilterPageResponse<TransportTicketFilterResponse>> searchTransportTickets(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "relevance") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        TransportTicketSearchRequest request = TransportTicketSearchRequest.builder()
                .keyword(keyword)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();

        return ApiResponse.<FilterPageResponse<TransportTicketFilterResponse>>builder()
                .message("Tìm kiếm vé phương tiện thành công!")
                .data(itemSearchService.searchTransportTickets(request))
                .build();
    }

    @GetMapping("/vouchers")
    public ApiResponse<FilterPageResponse<VoucherFilterResponse>> searchVouchers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "relevance") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        VoucherSearchRequest request = VoucherSearchRequest.builder()
                .keyword(keyword)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();

        return ApiResponse.<FilterPageResponse<VoucherFilterResponse>>builder()
                .message("Tìm kiếm voucher thành công!")
                .data(itemSearchService.searchVouchers(request))
                .build();
    }
}