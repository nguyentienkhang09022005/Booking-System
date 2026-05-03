package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.AccommodationFilterRequest;
import com.example.java_service.Dto.Request.TourFilterRequest;
import com.example.java_service.Dto.Request.TransportTicketFilterRequest;
import com.example.java_service.Dto.Request.VoucherFilterRequest;
import com.example.java_service.Dto.Response.AccommodationFilterResponse;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.FilterPageResponse;
import com.example.java_service.Dto.Response.TourFilterResponse;
import com.example.java_service.Dto.Response.TransportTicketFilterResponse;
import com.example.java_service.Dto.Response.VoucherFilterResponse;
import com.example.java_service.Services.ItemFilterService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/item-filter")
public class ItemFilterController {

    private final ItemFilterService itemFilterService;

    @GetMapping("/accommodations")
    public ApiResponse<FilterPageResponse<AccommodationFilterResponse>> filterAccommodations(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String accommodationType,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        AccommodationFilterRequest request = AccommodationFilterRequest.builder()
                .keyword(keyword)
                .accommodationType(accommodationType)
                .province(province)
                .city(city)
                .ward(ward)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .page(page)
                .size(size)
                .build();

        return ApiResponse.<FilterPageResponse<AccommodationFilterResponse>>builder()
                .message("Lọc nơi cư trú thành công!")
                .data(itemFilterService.filterAccommodations(request))
                .build();
    }

    @GetMapping("/tours")
    public ApiResponse<FilterPageResponse<TourFilterResponse>> filterTours(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minAdultPrice,
            @RequestParam(required = false) BigDecimal maxAdultPrice,
            @RequestParam(required = false) BigDecimal minChildPrice,
            @RequestParam(required = false) BigDecimal maxChildPrice,
            @RequestParam(required = false) Integer minDuration,
            @RequestParam(required = false) Integer maxDuration,
            @RequestParam(required = false) String durationType,
            @RequestParam(required = false) String languages,
            @RequestParam(required = false) String departureLocation,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String tourType,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        TourFilterRequest request = TourFilterRequest.builder()
                .keyword(keyword)
                .minAdultPrice(minAdultPrice)
                .maxAdultPrice(maxAdultPrice)
                .minChildPrice(minChildPrice)
                .maxChildPrice(maxChildPrice)
                .minDuration(minDuration)
                .maxDuration(maxDuration)
                .durationType(durationType)
                .languages(languages)
                .departureLocation(departureLocation)
                .destination(destination)
                .tourType(tourType)
                .difficulty(difficulty)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .page(page)
                .size(size)
                .build();

        return ApiResponse.<FilterPageResponse<TourFilterResponse>>builder()
                .message("Lọc tour du lịch thành công!")
                .data(itemFilterService.filterTours(request))
                .build();
    }

    @GetMapping("/transport-tickets")
    public ApiResponse<FilterPageResponse<TransportTicketFilterResponse>> filterTransportTickets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String ticketType,
            @RequestParam(required = false) String ticketClass,
            @RequestParam(required = false) String providerName,
            @RequestParam(required = false) String departureLocation,
            @RequestParam(required = false) String arrivalLocation,
            @RequestParam(required = false) LocalDateTime departureFrom,
            @RequestParam(required = false) LocalDateTime departureTo,
            @RequestParam(required = false) LocalDateTime arrivalFrom,
            @RequestParam(required = false) LocalDateTime arrivalTo,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minAvailableSlots,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        TransportTicketFilterRequest request = TransportTicketFilterRequest.builder()
                .keyword(keyword)
                .ticketType(ticketType)
                .ticketClass(ticketClass)
                .providerName(providerName)
                .departureLocation(departureLocation)
                .arrivalLocation(arrivalLocation)
                .departureFrom(departureFrom)
                .departureTo(departureTo)
                .arrivalFrom(arrivalFrom)
                .arrivalTo(arrivalTo)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minAvailableSlots(minAvailableSlots)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .page(page)
                .size(size)
                .build();

        return ApiResponse.<FilterPageResponse<TransportTicketFilterResponse>>builder()
                .message("Lọc vé phương tiện thành công!")
                .data(itemFilterService.filterTransportTickets(request))
                .build();
    }

    @GetMapping("/vouchers")
    public ApiResponse<FilterPageResponse<VoucherFilterResponse>> filterVouchers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) BigDecimal minDiscountPercent,
            @RequestParam(required = false) BigDecimal maxDiscountPercent,
            @RequestParam(required = false) BigDecimal minDiscountAmount,
            @RequestParam(required = false) BigDecimal maxDiscountAmount,
            @RequestParam(required = false) String sponsorType,
            @RequestParam(required = false) String applyTo,
            @RequestParam(required = false) LocalDate validFrom,
            @RequestParam(required = false) LocalDate validTo,
            @RequestParam(required = false) Integer minQuantity,
            @RequestParam(required = false) UUID accommodationId,
            @RequestParam(required = false) Boolean expireSoon,
            @RequestParam(required = false) Boolean newVoucher,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        VoucherFilterRequest request = VoucherFilterRequest.builder()
                .keyword(keyword)
                .code(code)
                .minDiscountPercent(minDiscountPercent)
                .maxDiscountPercent(maxDiscountPercent)
                .minDiscountAmount(minDiscountAmount)
                .maxDiscountAmount(maxDiscountAmount)
                .sponsorType(sponsorType)
                .applyTo(applyTo)
                .validFrom(validFrom)
                .validTo(validTo)
                .minQuantity(minQuantity)
                .accommodationId(accommodationId)
                .expireSoon(expireSoon)
                .newVoucher(newVoucher)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .page(page)
                .size(size)
                .build();

        return ApiResponse.<FilterPageResponse<VoucherFilterResponse>>builder()
                .message("Lọc voucher thành công!")
                .data(itemFilterService.filterVouchers(request))
                .build();
    }
}
