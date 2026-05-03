package com.example.java_service.Services;

import com.example.java_service.Constant.FilterConstant;
import com.example.java_service.Dto.Request.AccommodationSearchRequest;
import com.example.java_service.Dto.Request.TourSearchRequest;
import com.example.java_service.Dto.Request.TransportTicketSearchRequest;
import com.example.java_service.Dto.Request.VoucherSearchRequest;
import com.example.java_service.Dto.Response.*;
import com.example.java_service.Models.Accommodation;
import com.example.java_service.Models.Images;
import com.example.java_service.Models.Tour;
import com.example.java_service.Models.TransportTicket;
import com.example.java_service.Models.Voucher;
import com.example.java_service.Repositories.Jpa.AccommodationSearchRepository;
import com.example.java_service.Repositories.Jpa.TourSearchRepository;
import com.example.java_service.Repositories.Jpa.TransportTicketSearchRepository;
import com.example.java_service.Repositories.Jpa.VoucherSearchRepository;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemSearchService {

    private final AccommodationSearchRepository accommodationSearchRepository;
    private final TourSearchRepository tourSearchRepository;
    private final TransportTicketSearchRepository transportTicketSearchRepository;
    private final VoucherSearchRepository voucherSearchRepository;
    private final ImagesRepository imagesRepository;
    private final FuzzySearchService fuzzySearchService;

    @Transactional(readOnly = true)
    public FilterPageResponse<AccommodationFilterResponse> searchAccommodations(AccommodationSearchRequest request) {
        int page = Math.max(0, request.getPage());
        int size = Math.min(Math.max(1, request.getSize()), FilterConstant.MAX_PAGE_SIZE);

        Sort sort = buildAccommodationSort(request.getSortBy(), request.getSortDir());
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Accommodation> pageResult = 
                accommodationSearchRepository.searchWithKeyword(request, pageable);

        List<Accommodation> accommodations = pageResult.getContent();

        Map<String, List<Images>> imagesMap = fetchImagesMap(accommodations);

        List<Accommodation> reRanked = fuzzySearchService.reRankByFuzzyScore(
                accommodations,
                request.getKeyword(),
                Accommodation::getAccommodationName,
                Accommodation::getCity,
                Accommodation::getAccommodationType,
                Accommodation::getProvince
        );

        List<AccommodationFilterResponse> content = reRanked.stream()
                .map(acc -> {
                    String refId = acc.getIdAccommodation().toString();
                    List<Images> accImages = imagesMap.getOrDefault(refId, List.of());
                    List<String> imageUrls = accImages.stream().map(Images::getUrl).collect(Collectors.toList());
                    return AccommodationFilterResponse.builder()
                            .idAccommodation(acc.getIdAccommodation())
                            .accommodationName(acc.getAccommodationName())
                            .accommodationType(acc.getAccommodationType())
                            .street(acc.getStreet())
                            .ward(acc.getWard())
                            .city(acc.getCity())
                            .province(acc.getProvince())
                            .checkinTime(acc.getCheckinTime())
                            .checkoutTime(acc.getCheckoutTime())
                            .description(acc.getDescription())
                            .imageUrls(imageUrls)
                            .build();
                })
                .toList();

        return FilterPageResponse.of(content, page, size, pageResult.getTotalElements());
    }

    @Transactional(readOnly = true)
    public FilterPageResponse<TourFilterResponse> searchTours(TourSearchRequest request) {
        int page = Math.max(0, request.getPage());
        int size = Math.min(Math.max(1, request.getSize()), FilterConstant.MAX_PAGE_SIZE);

        Sort sort = buildTourSort(request.getSortBy(), request.getSortDir());
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Tour> pageResult = 
                tourSearchRepository.searchWithKeyword(request, pageable);

        List<Tour> tours = pageResult.getContent();

        Map<String, List<String>> imagesMap = fetchTourImagesMap(tours);

        List<Tour> reRanked = fuzzySearchService.reRankByFuzzyScore(
                tours,
                request.getKeyword(),
                Tour::getTourName,
                Tour::getDropOffPoint,
                Tour::getMeetingPoint
        );

        List<TourFilterResponse> content = reRanked.stream()
                .map(tour -> {
                    String refId = tour.getIdTour().toString();
                    List<String> imageUrls = imagesMap.getOrDefault(refId, List.of());
                    List<TourScheduleResponse> schedules = tour.getTourSchedules() != null
                            ? tour.getTourSchedules().stream()
                                    .map(s -> TourScheduleResponse.builder()
                                            .idSchedule(s.getIdSchedule())
                                            .departureDate(s.getDepartureDate())
                                            .departureTime(s.getDepartureTime())
                                            .arrivalLocation(s.getArrivalLocation())
                                            .description(s.getDescription())
                                            .build())
                                    .collect(Collectors.toList())
                            : List.of();
                    return TourFilterResponse.builder()
                            .idTour(tour.getIdTour())
                            .tourName(tour.getTourName())
                            .description(tour.getDescription())
                            .durationMinutes(tour.getDurationMinutes())
                            .meetingPoint(tour.getMeetingPoint())
                            .dropOffPoint(tour.getDropOffPoint())
                            .adultPrice(tour.getAdultPrice())
                            .childPrice(tour.getChildPrice())
                            .languages(tour.getLanguages())
                            .inclusions(tour.getInclusions())
                            .exclusions(tour.getExclusions())
                            .additionalInfo(tour.getAdditionalInfo())
                            .status(tour.getStatus())
                            .createdAt(tour.getCreatedAt())
                            .tourSchedules(schedules)
                            .imageUrls(imageUrls)
                            .build();
                })
                .toList();

        return FilterPageResponse.of(content, page, size, pageResult.getTotalElements());
    }

    @Transactional(readOnly = true)
    public FilterPageResponse<TransportTicketFilterResponse> searchTransportTickets(TransportTicketSearchRequest request) {
        int page = Math.max(0, request.getPage());
        int size = Math.min(Math.max(1, request.getSize()), FilterConstant.MAX_PAGE_SIZE);

        Sort sort = buildTransportTicketSort(request.getSortBy(), request.getSortDir());
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TransportTicket> pageResult = 
                transportTicketSearchRepository.searchWithKeyword(request, pageable);

        List<TransportTicket> tickets = pageResult.getContent();

        List<TransportTicket> reRanked = fuzzySearchService.reRankByFuzzyScore(
                tickets,
                request.getKeyword(),
                TransportTicket::getProviderName,
                TransportTicket::getDepartureLocation,
                TransportTicket::getArrivalLocation,
                TransportTicket::getTicketType
        );

        List<TransportTicketFilterResponse> content = reRanked.stream()
                .map(ticket -> TransportTicketFilterResponse.builder()
                        .idTicket(ticket.getIdTicket())
                        .ticketType(ticket.getTicketType())
                        .ticketClass(ticket.getTicketClass())
                        .providerName(ticket.getProviderName())
                        .departureLocation(ticket.getDepartureLocation())
                        .arrivalLocation(ticket.getArrivalLocation())
                        .departureTime(ticket.getDepartureTime())
                        .arrivalTime(ticket.getArrivalTime())
                        .price(ticket.getPrice())
                        .status(ticket.getStatus())
                        .createdAt(ticket.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return FilterPageResponse.of(content, page, size, pageResult.getTotalElements());
    }

    @Transactional(readOnly = true)
    public FilterPageResponse<VoucherFilterResponse> searchVouchers(VoucherSearchRequest request) {
        int page = Math.max(0, request.getPage());
        int size = Math.min(Math.max(1, request.getSize()), FilterConstant.MAX_PAGE_SIZE);

        Sort sort = buildVoucherSort(request.getSortBy(), request.getSortDir());
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Voucher> pageResult = 
                voucherSearchRepository.searchWithKeyword(request, pageable);

        List<Voucher> vouchers = pageResult.getContent();

        Map<String, String> imagesMap = fetchVoucherImagesMap(vouchers);

        List<Voucher> reRanked = fuzzySearchService.reRankByFuzzyScore(
                vouchers,
                request.getKeyword(),
                Voucher::getNameVoucher,
                Voucher::getSponsorType,
                Voucher::getApplyTo
        );

        List<VoucherFilterResponse> content = reRanked.stream()
                .map(voucher -> {
                    String refId = voucher.getIdVoucher().toString();
                    String imageUrl = imagesMap.get(refId);
                    return VoucherFilterResponse.builder()
                            .idVoucher(voucher.getIdVoucher())
                            .nameVoucher(voucher.getNameVoucher())
                            .discountPercent(voucher.getDiscountPercent())
                            .minDiscountAmount(voucher.getMinDiscountAmount())
                            .maxDiscountAmount(voucher.getMaxDiscountAmount())
                            .sponsorType(voucher.getSponsorType())
                            .applyTo(voucher.getApplyTo())
                            .startDate(voucher.getStartDate())
                            .endDate(voucher.getEndDate())
                            .quantity(voucher.getQuantity())
                            .conditions(voucher.getConditions())
                            .createAt(voucher.getCreateAt())
                            .imageUrl(imageUrl)
                            .build();
                })
                .toList();

        return FilterPageResponse.of(content, page, size, pageResult.getTotalElements());
    }

    private Map<String, List<Images>> fetchImagesMap(List<Accommodation> accommodations) {
        if (accommodations.isEmpty()) {
            return Map.of();
        }
        List<String> refIds = accommodations.stream()
                .map(acc -> acc.getIdAccommodation().toString())
                .toList();
        List<Images> allImages = imagesRepository.findByRefIdIn(refIds);
        return allImages.stream()
                .collect(Collectors.groupingBy(Images::getRefId));
    }

    private Map<String, List<String>> fetchTourImagesMap(List<Tour> tours) {
        if (tours.isEmpty()) {
            return Map.of();
        }
        List<String> refIds = tours.stream()
                .map(tour -> tour.getIdTour().toString())
                .toList();
        List<Images> allImages = imagesRepository.findByRefIdIn(refIds);
        return allImages.stream()
                .collect(Collectors.groupingBy(
                        Images::getRefId,
                        Collectors.mapping(Images::getUrl, Collectors.toList())
                ));
    }

    private Map<String, String> fetchVoucherImagesMap(List<Voucher> vouchers) {
        if (vouchers.isEmpty()) {
            return Map.of();
        }
        List<String> refIds = vouchers.stream()
                .map(voucher -> voucher.getIdVoucher().toString())
                .toList();
        List<Images> allImages = imagesRepository.findByRefIdIn(refIds);
        return allImages.stream()
                .collect(Collectors.toMap(
                        Images::getRefId,
                        Images::getUrl,
                        (existing, replacement) -> existing
                ));
    }

    private Sort buildAccommodationSort(String sortBy, String sortDir) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        if (sortBy == null || FilterConstant.SORT_RELEVANCE.equalsIgnoreCase(sortBy) 
                || !FilterConstant.AccommodationSortBy.ALLOWED.contains(sortBy)) {
            return Sort.by(direction, "createdAt");
        }
        return Sort.by(direction, sortBy);
    }

    private Sort buildTourSort(String sortBy, String sortDir) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        if (sortBy == null || FilterConstant.SORT_RELEVANCE.equalsIgnoreCase(sortBy) 
                || !FilterConstant.TourSortBy.ALLOWED.contains(sortBy)) {
            return Sort.by(direction, "createdAt");
        }
        return Sort.by(direction, sortBy);
    }

    private Sort buildTransportTicketSort(String sortBy, String sortDir) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        if (sortBy == null || FilterConstant.SORT_RELEVANCE.equalsIgnoreCase(sortBy) 
                || !FilterConstant.TransportTicketSortBy.ALLOWED.contains(sortBy)) {
            return Sort.by(direction, "createdAt");
        }
        return Sort.by(direction, sortBy);
    }

    private Sort buildVoucherSort(String sortBy, String sortDir) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        if (sortBy == null || FilterConstant.SORT_RELEVANCE.equalsIgnoreCase(sortBy) 
                || !FilterConstant.VoucherSortBy.ALLOWED.contains(sortBy)) {
            return Sort.by(direction, "createdAt");
        }
        return Sort.by(direction, sortBy);
    }

    private boolean shouldReRank(String sortBy) {
        return sortBy == null || FilterConstant.SORT_RELEVANCE.equalsIgnoreCase(sortBy);
    }
}