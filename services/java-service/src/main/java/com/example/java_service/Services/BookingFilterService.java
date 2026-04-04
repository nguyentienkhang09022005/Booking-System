package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedApplyTo;
import com.example.java_service.Constant.PredefinedSponsorType;
import com.example.java_service.Dto.Response.*;
import com.example.java_service.Mappers.BookingMapper;
import com.example.java_service.Mappers.BookingTourMapper;
import com.example.java_service.Mappers.BookingTransportTicketMapper;
import com.example.java_service.Mappers.VoucherMapper;
import com.example.java_service.Models.*;
import com.example.java_service.Repositories.Jpa.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingFilterService {

    AccommodationRepository accommodationRepository;
    TourRepository tourRepository;
    TransportTicketRepository transportTicketRepository;

    BookingRepository bookingRepository;
    BookingRoomRepository bookingRoomRepository;
    BookingServiceRepository bookingServiceRepository;

    BookingTourRepository bookingTourRepository;
    BookingTransportTicketRepository bookingTransportTicketRepository;

    AppliedVoucherRepository appliedVoucherRepository;

    BookingMapper bookingMapper;
    BookingTourMapper bookingTourMapper;
    BookingTransportTicketMapper bookingTransportTicketMapper;
    VoucherMapper voucherMapper;

    public List<SimpleItemResponse> getSimpleAccommodations() {
        return accommodationRepository.findAllSimpleAccommodations();
    }

    public List<SimpleItemResponse> getSimpleTours() {
        return tourRepository.findAllSimpleTours();
    }

    public List<SimpleItemResponse> getSimpleTransportTickets() {
        return transportTicketRepository.findAllSimpleTransportTickets();
    }

    public BookingFilterResponse<BookingListResponse> filterAccommodationBookings(UUID idAccommodation, int month, int year) {
        List<Booking> bookings = bookingRepository.findByAccommodationAndMonthYear(idAccommodation, month, year);
        if (bookings.isEmpty()) {
            return new BookingFilterResponse<>(List.of(), List.of());
        }

        List<UUID> bookingIds = bookings.stream().map(Booking::getIdBooking).toList();

        List<BookingRoom> allBookingRooms = bookingRoomRepository.findAllBookingRoomByBookingIds(bookingIds);
        List<BookingService> allBookingServices = bookingServiceRepository.findAllBookingServiceByBookingIds(bookingIds);
        List<AppliedVoucher> allAppliedVouchers = appliedVoucherRepository.findAppliedVouchersWithVoucherByBookingIds(bookingIds);

        Map<UUID, List<BookingRoom>> roomMap = allBookingRooms.stream().collect(Collectors.groupingBy(br -> br.getBooking().getIdBooking()));
        Map<UUID, List<BookingService>> serviceMap = allBookingServices.stream().collect(Collectors.groupingBy(bs -> bs.getBooking().getIdBooking()));
        Map<UUID, List<AppliedVoucher>> voucherMap = allAppliedVouchers.stream().collect(Collectors.groupingBy(av -> av.getBooking().getIdBooking()));

        Map<String, Object[]> statsMap = new HashMap<>();
        statsMap.put(PredefinedApplyTo.APPLY_TO_ALL + "_" + PredefinedSponsorType.SYSTEM_TYPE, new Object[]{0, BigDecimal.ZERO});
        statsMap.put(PredefinedApplyTo.APPLY_TO_ALL + "_" + PredefinedSponsorType.BUSINESS_TYPE, new Object[]{0, BigDecimal.ZERO});
        statsMap.put(PredefinedApplyTo.APPLY_TO_ROOM + "_" + PredefinedSponsorType.SYSTEM_TYPE, new Object[]{0, BigDecimal.ZERO});
        statsMap.put(PredefinedApplyTo.APPLY_TO_ROOM + "_" + PredefinedSponsorType.BUSINESS_TYPE, new Object[]{0, BigDecimal.ZERO});
        statsMap.put(PredefinedApplyTo.APPLY_TO_SERVICE + "_" + PredefinedSponsorType.SYSTEM_TYPE, new Object[]{0, BigDecimal.ZERO});
        statsMap.put(PredefinedApplyTo.APPLY_TO_SERVICE + "_" + PredefinedSponsorType.BUSINESS_TYPE, new Object[]{0, BigDecimal.ZERO});

        for (Booking booking : bookings) {
            UUID bId = booking.getIdBooking();
            List<AppliedVoucher> appliedVouchers = voucherMap.getOrDefault(bId, List.of());

            if (appliedVouchers.isEmpty()) continue;

            BigDecimal totalPriceRoom = roomMap.getOrDefault(bId, List.of()).stream()
                    .map(BookingRoom::getPriceBookingRoom)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long daysBetween = ChronoUnit.DAYS.between(booking.getCheckinDate().toLocalDate(), booking.getCheckoutDate().toLocalDate());
            BigDecimal finalRoomPrice = (daysBetween >= 1) ? totalPriceRoom.multiply(BigDecimal.valueOf(daysBetween)) : totalPriceRoom;

            BigDecimal remainingServicePrice = serviceMap.getOrDefault(bId, List.of()).stream()
                    .map(BookingService::getPriceBookingService)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remainingRoomPrice = finalRoomPrice;

            for (AppliedVoucher av : appliedVouchers) {
                Voucher voucher = av.getVoucher();
                if (voucher.getDiscountPercent() == null || voucher.getDiscountPercent().compareTo(BigDecimal.ZERO) <= 0) continue;

                String applyTo = voucher.getApplyTo() != null ? voucher.getApplyTo().toUpperCase() : PredefinedApplyTo.APPLY_TO_ALL;
                String sponsorType = voucher.getSponsorType() != null ? voucher.getSponsorType().toUpperCase() : PredefinedSponsorType.SYSTEM_TYPE;
                String statKey = applyTo + "_" + sponsorType;
                BigDecimal targetPrice = BigDecimal.ZERO;

                switch (applyTo) {
                    case PredefinedApplyTo.APPLY_TO_ROOM: targetPrice = remainingRoomPrice; break;
                    case PredefinedApplyTo.APPLY_TO_SERVICE: targetPrice = remainingServicePrice; break;
                    default: targetPrice = remainingRoomPrice.add(remainingServicePrice); break;
                }

                if (targetPrice.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal discount = targetPrice.multiply(voucher.getDiscountPercent()).divide(BigDecimal.valueOf(100), 10, RoundingMode.DOWN);

                if (voucher.getMinDiscountAmount() != null && discount.compareTo(voucher.getMinDiscountAmount()) < 0) discount = voucher.getMinDiscountAmount();
                if (voucher.getMaxDiscountAmount() != null && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) discount = voucher.getMaxDiscountAmount();
                if (discount.compareTo(targetPrice) > 0) discount = targetPrice;

                switch (applyTo) {
                    case PredefinedApplyTo.APPLY_TO_ROOM: remainingRoomPrice = remainingRoomPrice.subtract(discount); break;
                    case PredefinedApplyTo.APPLY_TO_SERVICE: remainingServicePrice = remainingServicePrice.subtract(discount); break;
                    default:
                        if (discount.compareTo(remainingRoomPrice) <= 0) remainingRoomPrice = remainingRoomPrice.subtract(discount);
                        else {
                            BigDecimal leftover = discount.subtract(remainingRoomPrice);
                            remainingRoomPrice = BigDecimal.ZERO;
                            remainingServicePrice = remainingServicePrice.subtract(leftover);
                        }
                        break;
                }

                Object[] stat = statsMap.get(statKey);
                if (stat == null) {
                    stat = new Object[]{0, BigDecimal.ZERO};
                    statsMap.put(statKey, stat);
                }
                stat[0] = (int) stat[0] + 1;
                stat[1] = ((BigDecimal) stat[1]).add(discount);
            }
        }

        List<VoucherStatisticResponse> voucherStats = statsMap.entrySet().stream()
                .filter(entry -> (int) entry.getValue()[0] > 0)
                .map(entry -> {
                    String[] parts = entry.getKey().split("_");
                    return VoucherStatisticResponse.builder()
                            .applyTo(parts[0])
                            .sponsorType(parts[1])
                            .quantityUsed((int) entry.getValue()[0])
                            .totalDiscountValue((BigDecimal) entry.getValue()[1])
                            .build();
                })
                .toList();

        List<BookingListResponse> bookingListResponses = bookings.stream()
                .map(bookingMapper::toBookingListResponse)
                .toList();

        return new BookingFilterResponse<>(bookingListResponses, voucherStats);
    }

    public BookingFilterResponse<BookingTourResponse> filterTourBookings(UUID idTour, int month, int year) {
        List<BookingTour> bookings = bookingTourRepository.findByTourAndMonthYear(idTour, month, year);
        if (bookings.isEmpty()) {
            return new BookingFilterResponse<>(List.of(), List.of());
        }

        List<UUID> bookingIds = bookings.stream().map(BookingTour::getIdBookingTour).toList();

        List<AppliedVoucher> allAppliedVouchers = appliedVoucherRepository.findAppliedVouchersWithVoucherByBookingTourIds(bookingIds);
        Map<UUID, List<AppliedVoucher>> voucherMap = allAppliedVouchers.stream()
                .collect(Collectors.groupingBy(av -> av.getBookingTour().getIdBookingTour()));

        Map<String, Object[]> statsMap = new HashMap<>();
        statsMap.put(PredefinedApplyTo.APPLY_TO_TOUR + "_" + PredefinedSponsorType.SYSTEM_TYPE, new Object[]{0, BigDecimal.ZERO});
        statsMap.put(PredefinedApplyTo.APPLY_TO_TOUR + "_" + PredefinedSponsorType.BUSINESS_TYPE, new Object[]{0, BigDecimal.ZERO});
        statsMap.put(PredefinedApplyTo.APPLY_TO_ALL + "_" + PredefinedSponsorType.SYSTEM_TYPE, new Object[]{0, BigDecimal.ZERO});
        statsMap.put(PredefinedApplyTo.APPLY_TO_ALL + "_" + PredefinedSponsorType.BUSINESS_TYPE, new Object[]{0, BigDecimal.ZERO});

        for (BookingTour booking : bookings) {
            UUID bId = booking.getIdBookingTour();
            List<AppliedVoucher> appliedVouchers = voucherMap.getOrDefault(bId, List.of());

            if (appliedVouchers.isEmpty()) continue;

            BigDecimal remainingPrice = booking.getPriceBookingTour();

            for (AppliedVoucher av : appliedVouchers) {
                Voucher voucher = av.getVoucher();
                if (voucher.getDiscountPercent() == null || voucher.getDiscountPercent().compareTo(BigDecimal.ZERO) <= 0) continue;

                String applyTo = voucher.getApplyTo() != null ? voucher.getApplyTo().toUpperCase() : PredefinedApplyTo.APPLY_TO_ALL;
                String sponsorType = voucher.getSponsorType() != null ? voucher.getSponsorType().toUpperCase() : PredefinedSponsorType.SYSTEM_TYPE;
                String statKey = applyTo + "_" + sponsorType;

                if (remainingPrice.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal discount = remainingPrice.multiply(voucher.getDiscountPercent()).divide(BigDecimal.valueOf(100), 10, RoundingMode.DOWN);

                if (voucher.getMinDiscountAmount() != null && discount.compareTo(voucher.getMinDiscountAmount()) < 0) discount = voucher.getMinDiscountAmount();
                if (voucher.getMaxDiscountAmount() != null && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) discount = voucher.getMaxDiscountAmount();
                if (discount.compareTo(remainingPrice) > 0) discount = remainingPrice;

                remainingPrice = remainingPrice.subtract(discount);

                Object[] stat = statsMap.get(statKey);
                if (stat == null) {
                    stat = new Object[]{0, BigDecimal.ZERO};
                    statsMap.put(statKey, stat);
                }
                stat[0] = (int) stat[0] + 1;
                stat[1] = ((BigDecimal) stat[1]).add(discount);
            }
        }

        List<VoucherStatisticResponse> voucherStats = statsMap.entrySet().stream()
                .filter(entry -> (int) entry.getValue()[0] > 0)
                .map(entry -> {
                    String[] parts = entry.getKey().split("_");
                    return VoucherStatisticResponse.builder()
                            .applyTo(parts[0])
                            .sponsorType(parts[1])
                            .quantityUsed((int) entry.getValue()[0])
                            .totalDiscountValue((BigDecimal) entry.getValue()[1])
                            .build();
                })
                .toList();

        List<BookingTourResponse> bookingResponses = bookings.stream()
                .map(b -> {
                    BookingTourResponse response = bookingTourMapper.toBookingTourResponse(b);
                    List<AppliedVoucher> avs = voucherMap.getOrDefault(b.getIdBookingTour(), List.of());

                    List<VoucherViewResponse> vRes = avs.stream()
                            .map(av -> voucherMapper.toVoucherViewResponse(av.getVoucher()))
                            .toList();

                    response.setVoucherViewResponses(vRes);
                    return response;
                })
                .toList();

        return new BookingFilterResponse<>(bookingResponses, voucherStats);
    }

    public BookingFilterResponse<BookingTransportResponse> filterTransportBookings(UUID idTicket, int month, int year) {
        List<BookingTransport> bookings = bookingTransportTicketRepository.findByTransportTicketAndMonthYear(idTicket, month, year);
        if (bookings.isEmpty()) {
            return new BookingFilterResponse<>(List.of(), List.of());
        }

        List<UUID> bookingIds = bookings.stream().map(BookingTransport::getIdBookingTransport).toList();

        List<AppliedVoucher> allAppliedVouchers = appliedVoucherRepository.findAppliedVouchersWithVoucherByBookingTransportIds(bookingIds);
        Map<UUID, List<AppliedVoucher>> voucherMap = allAppliedVouchers.stream()
                .collect(Collectors.groupingBy(av -> av.getBookingTransport().getIdBookingTransport()));

        Map<String, Object[]> statsMap = new HashMap<>();
        statsMap.put("TICKET_" + PredefinedSponsorType.SYSTEM_TYPE, new Object[]{0, BigDecimal.ZERO});
        statsMap.put("TICKET_" + PredefinedSponsorType.BUSINESS_TYPE, new Object[]{0, BigDecimal.ZERO});
        statsMap.put(PredefinedApplyTo.APPLY_TO_ALL + "_" + PredefinedSponsorType.SYSTEM_TYPE, new Object[]{0, BigDecimal.ZERO});
        statsMap.put(PredefinedApplyTo.APPLY_TO_ALL + "_" + PredefinedSponsorType.BUSINESS_TYPE, new Object[]{0, BigDecimal.ZERO});

        for (BookingTransport booking : bookings) {
            UUID bId = booking.getIdBookingTransport();
            List<AppliedVoucher> appliedVouchers = voucherMap.getOrDefault(bId, List.of());

            if (appliedVouchers.isEmpty()) continue;

            BigDecimal remainingPrice = booking.getPriceBookingTransport();

            for (AppliedVoucher av : appliedVouchers) {
                Voucher voucher = av.getVoucher();
                if (voucher.getDiscountPercent() == null || voucher.getDiscountPercent().compareTo(BigDecimal.ZERO) <= 0) continue;

                String applyTo = voucher.getApplyTo() != null ? voucher.getApplyTo().toUpperCase() : PredefinedApplyTo.APPLY_TO_ALL;
                String sponsorType = voucher.getSponsorType() != null ? voucher.getSponsorType().toUpperCase() : PredefinedSponsorType.SYSTEM_TYPE;
                String statKey = applyTo + "_" + sponsorType;

                if (remainingPrice.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal discount = remainingPrice.multiply(voucher.getDiscountPercent()).divide(BigDecimal.valueOf(100), 10, RoundingMode.DOWN);

                if (voucher.getMinDiscountAmount() != null && discount.compareTo(voucher.getMinDiscountAmount()) < 0) discount = voucher.getMinDiscountAmount();
                if (voucher.getMaxDiscountAmount() != null && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) discount = voucher.getMaxDiscountAmount();
                if (discount.compareTo(remainingPrice) > 0) discount = remainingPrice;

                remainingPrice = remainingPrice.subtract(discount);

                Object[] stat = statsMap.get(statKey);
                if (stat == null) {
                    stat = new Object[]{0, BigDecimal.ZERO};
                    statsMap.put(statKey, stat);
                }
                stat[0] = (int) stat[0] + 1;
                stat[1] = ((BigDecimal) stat[1]).add(discount);
            }
        }

        List<VoucherStatisticResponse> voucherStats = statsMap.entrySet().stream()
                .filter(entry -> (int) entry.getValue()[0] > 0)
                .map(entry -> {
                    String[] parts = entry.getKey().split("_");
                    return VoucherStatisticResponse.builder()
                            .applyTo(parts[0])
                            .sponsorType(parts[1])
                            .quantityUsed((int) entry.getValue()[0])
                            .totalDiscountValue((BigDecimal) entry.getValue()[1])
                            .build();
                })
                .toList();

        List<BookingTransportResponse> bookingResponses = bookings.stream()
                .map(b -> {
                    BookingTransportResponse response = bookingTransportTicketMapper.toBookingTransportResponse(b);
                    List<AppliedVoucher> avs = voucherMap.getOrDefault(b.getIdBookingTransport(), List.of());

                    List<VoucherViewResponse> vRes = avs.stream()
                            .map(av -> voucherMapper.toVoucherViewResponse(av.getVoucher()))
                            .toList();

                    response.setVoucherViewResponses(vRes);
                    return response;
                })
                .toList();

        return new BookingFilterResponse<>(bookingResponses, voucherStats);
    }
}