package com.example.java_service.Services;

import com.example.java_service.Dto.Response.SegmentChartResponse;
import com.example.java_service.Dto.Response.SegmentDistributionResponse;
import com.example.java_service.Models.Booking;
import com.example.java_service.Repositories.Jpa.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BusinessAnalyticsService {

    private final BookingRepository bookingRepository;

    private static final List<String> SEGMENTS = List.of("FAMILY", "COUPLES", "GROUP", "TOURIST", "BUSINESS");
    private static final int DEFAULT_MONTHS = 6;

    @Cacheable(value = "segmentDistribution", key = "#businessId + '-' + #months")
    public SegmentDistributionResponse getSegmentDistribution(UUID businessId, Integer months) {
        int monthsToUse = months != null && months > 0 ? months : DEFAULT_MONTHS;

        List<Object[]> rawData = bookingRepository.getSegmentDistributionByBusiness(businessId);

        Map<String, SegmentDistributionResponse.SegmentStatsDTO> segmentStats = new LinkedHashMap<>();
        long totalCustomers = 0;
        double totalRevenue = 0;

        for (Object[] row : rawData) {
            String segment = (String) row[0];
            long count = ((Number) row[1]).longValue();
            double revenue = row[2] != null ? ((Number) row[2]).doubleValue() : 0;

            if (segment != null) {
                segmentStats.put(segment, SegmentDistributionResponse.SegmentStatsDTO.builder()
                        .count(count)
                        .totalRevenue(Math.round(revenue))
                        .avgBookingValue(count > 0 ? (int) Math.round(revenue / count) : 0)
                        .percentage(0.0)
                        .build());
                totalCustomers += count;
                totalRevenue += revenue;
            }
        }

        for (String segment : SEGMENTS) {
            if (!segmentStats.containsKey(segment)) {
                segmentStats.put(segment, SegmentDistributionResponse.SegmentStatsDTO.builder()
                        .count(0L)
                        .totalRevenue(0L)
                        .avgBookingValue(0)
                        .percentage(0.0)
                        .build());
            }
        }

        final long finalTotal = totalCustomers;
        segmentStats.replaceAll((seg, stats) -> SegmentDistributionResponse.SegmentStatsDTO.builder()
                .count(stats.getCount())
                .totalRevenue(stats.getTotalRevenue())
                .avgBookingValue(stats.getAvgBookingValue())
                .percentage(finalTotal > 0 ? Math.round((double) stats.getCount() / finalTotal * 1000) / 10.0 : 0)
                .build());

        return SegmentDistributionResponse.builder()
                .periodMonths(monthsToUse)
                .segments(segmentStats)
                .totalCustomers(totalCustomers)
                .totalRevenue(Math.round(totalRevenue))
                .build();
    }

    public SegmentChartResponse getChartData(UUID businessId, Integer months) {
        int monthsToUse = months != null && months > 0 ? months : DEFAULT_MONTHS;
        LocalDateTime now = LocalDateTime.now();

        List<String> labels = new ArrayList<>();
        Map<String, List<Integer>> datasets = new LinkedHashMap<>();

        for (String segment : SEGMENTS) {
            datasets.put(segment, new ArrayList<>());
        }

        for (int i = monthsToUse - 1; i >= 0; i--) {
            YearMonth ym = YearMonth.from(now.minusMonths(i));
            labels.add(ym.format(DateTimeFormatter.ofPattern("MMM")));

            LocalDateTime startOfMonth = ym.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = ym.atEndOfMonth().atTime(23, 59, 59);

            List<Booking> monthBookings = bookingRepository.findByBusinessIdAndDateRange(businessId, startOfMonth, endOfMonth);

            Map<String, Integer> segmentCounts = new LinkedHashMap<>();
            for (String segment : SEGMENTS) {
                segmentCounts.put(segment, 0);
            }

            for (Booking booking : monthBookings) {
                if (booking.getUsers() != null && booking.getUsers().getCustomerSegment() != null) {
                    String segment = booking.getUsers().getCustomerSegment();
                    segmentCounts.merge(segment, 1, Integer::sum);
                }
            }

            for (String segment : SEGMENTS) {
                datasets.get(segment).add(segmentCounts.get(segment));
            }
        }

        return SegmentChartResponse.builder()
                .labels(labels)
                .datasets(datasets)
                .build();
    }
}