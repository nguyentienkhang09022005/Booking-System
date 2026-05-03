package com.example.java_service.Services;

import com.example.java_service.Constant.FilterConstant;
import com.example.java_service.Dto.Request.AccommodationFilterRequest;
import com.example.java_service.Dto.Request.TourFilterRequest;
import com.example.java_service.Dto.Request.TransportTicketFilterRequest;
import com.example.java_service.Dto.Request.VoucherFilterRequest;
import com.example.java_service.Dto.Response.AccommodationFilterResponse;
import com.example.java_service.Dto.Response.FilterPageResponse;
import com.example.java_service.Dto.Response.TourFilterResponse;
import com.example.java_service.Dto.Response.TransportTicketFilterResponse;
import com.example.java_service.Dto.Response.VoucherFilterResponse;
import com.example.java_service.Mappers.AccommodationFilterMapper;
import com.example.java_service.Mappers.TourFilterMapper;
import com.example.java_service.Mappers.TransportTicketFilterMapper;
import com.example.java_service.Mappers.VoucherFilterMapper;
import com.example.java_service.Models.Images;
import com.example.java_service.Repositories.Jpa.AccommodationFilterRepository;
import com.example.java_service.Repositories.Jpa.TourFilterRepository;
import com.example.java_service.Repositories.Jpa.TransportTicketFilterRepository;
import com.example.java_service.Repositories.Jpa.VoucherFilterRepository;
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
public class ItemFilterService {

    private final AccommodationFilterRepository accommodationFilterRepository;
    private final TourFilterRepository tourFilterRepository;
    private final TransportTicketFilterRepository transportTicketFilterRepository;
    private final VoucherFilterRepository voucherFilterRepository;
    private final ImagesRepository imagesRepository;
    private final AccommodationFilterMapper accommodationFilterMapper;
    private final TourFilterMapper tourFilterMapper;
    private final TransportTicketFilterMapper transportTicketFilterMapper;
    private final VoucherFilterMapper voucherFilterMapper;

    @Transactional(readOnly = true)
    public FilterPageResponse<AccommodationFilterResponse> filterAccommodations(AccommodationFilterRequest request) {
        int page = Math.max(0, request.getPage());
        int size = Math.min(Math.max(1, request.getSize()), FilterConstant.MAX_PAGE_SIZE);

        Sort sort = buildAccommodationSort(request.getSortBy(), request.getSortDir());
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<com.example.java_service.Models.Accommodation> pageResult = accommodationFilterRepository.findAllWithFilters(request, pageable);

        List<com.example.java_service.Models.Accommodation> accommodations = pageResult.getContent();

        Map<String, List<Images>> imagesMap = fetchImagesMap(accommodations);

        List<AccommodationFilterResponse> content = accommodations.stream()
                .map(acc -> {
                    String refId = acc.getIdAccommodation().toString();
                    List<Images> accImages = imagesMap.getOrDefault(refId, List.of());
                    return accommodationFilterMapper.toResponse(acc, accImages);
                })
                .toList();

        return FilterPageResponse.of(content, page, size, pageResult.getTotalElements());
    }

    @Transactional(readOnly = true)
    public FilterPageResponse<TourFilterResponse> filterTours(TourFilterRequest request) {
        int page = Math.max(0, request.getPage());
        int size = Math.min(Math.max(1, request.getSize()), FilterConstant.MAX_PAGE_SIZE);

        Sort sort = buildTourSort(request.getSortBy(), request.getSortDir());
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<com.example.java_service.Models.Tour> pageResult = tourFilterRepository.findAllWithFilters(request, pageable);

        List<com.example.java_service.Models.Tour> tours = pageResult.getContent();

        Map<String, List<String>> imagesMap = fetchTourImagesMap(tours);

        List<TourFilterResponse> content = tours.stream()
                .map(tour -> {
                    String refId = tour.getIdTour().toString();
                    List<String> imageUrls = imagesMap.getOrDefault(refId, List.of());
                    return tourFilterMapper.toResponse(tour, imageUrls);
                })
                .toList();

        return FilterPageResponse.of(content, page, size, pageResult.getTotalElements());
    }

    @Transactional(readOnly = true)
    public FilterPageResponse<TransportTicketFilterResponse> filterTransportTickets(TransportTicketFilterRequest request) {
        int page = Math.max(0, request.getPage());
        int size = Math.min(Math.max(1, request.getSize()), FilterConstant.MAX_PAGE_SIZE);

        Sort sort = buildTransportTicketSort(request.getSortBy(), request.getSortDir());
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<com.example.java_service.Models.TransportTicket> pageResult = transportTicketFilterRepository.findAllWithFilters(request, pageable);

        List<TransportTicketFilterResponse> content = pageResult.getContent().stream()
                .map(transportTicketFilterMapper::toResponse)
                .toList();

        return FilterPageResponse.of(content, page, size, pageResult.getTotalElements());
    }

    @Transactional(readOnly = true)
    public FilterPageResponse<VoucherFilterResponse> filterVouchers(VoucherFilterRequest request) {
        int page = Math.max(0, request.getPage());
        int size = Math.min(Math.max(1, request.getSize()), FilterConstant.MAX_PAGE_SIZE);

        Sort sort = buildVoucherSort(request.getSortBy(), request.getSortDir());
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<com.example.java_service.Models.Voucher> pageResult = voucherFilterRepository.findAllWithFilters(request, pageable);

        List<com.example.java_service.Models.Voucher> vouchers = pageResult.getContent();

        Map<String, String> imagesMap = fetchVoucherImagesMap(vouchers);

        List<VoucherFilterResponse> content = vouchers.stream()
                .map(voucher -> {
                    String refId = voucher.getIdVoucher().toString();
                    String imageUrl = imagesMap.get(refId);
                    return voucherFilterMapper.toResponse(voucher, imageUrl);
                })
                .toList();

        return FilterPageResponse.of(content, page, size, pageResult.getTotalElements());
    }

    private Map<String, List<Images>> fetchImagesMap(List<com.example.java_service.Models.Accommodation> accommodations) {
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

    private Map<String, List<String>> fetchTourImagesMap(List<com.example.java_service.Models.Tour> tours) {
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

    private Map<String, String> fetchVoucherImagesMap(List<com.example.java_service.Models.Voucher> vouchers) {
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
        String field = (sortBy != null && FilterConstant.AccommodationSortBy.ALLOWED.contains(sortBy))
                ? sortBy
                : FilterConstant.AccommodationSortBy.ACCOMMODATION_NAME;
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }

    private Sort buildTourSort(String sortBy, String sortDir) {
        String field = (sortBy != null && FilterConstant.TourSortBy.ALLOWED.contains(sortBy))
                ? sortBy
                : FilterConstant.TourSortBy.TOUR_NAME;
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }

    private Sort buildTransportTicketSort(String sortBy, String sortDir) {
        String field = (sortBy != null && FilterConstant.TransportTicketSortBy.ALLOWED.contains(sortBy))
                ? sortBy
                : FilterConstant.TransportTicketSortBy.PROVIDER_NAME;
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }

    private Sort buildVoucherSort(String sortBy, String sortDir) {
        String field = (sortBy != null && FilterConstant.VoucherSortBy.ALLOWED.contains(sortBy))
                ? sortBy
                : FilterConstant.VoucherSortBy.NAME_VOUCHER;
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }
}
