package com.example.java_service.Services;

import com.example.java_service.Dto.Request.AppliedVoucherRequest;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.AppliedVoucherMapper;
import com.example.java_service.Models.AppliedVoucher;
import com.example.java_service.Models.Booking;
import com.example.java_service.Models.BookingTour;
import com.example.java_service.Models.BookingTransport;
import com.example.java_service.Models.Users;
import com.example.java_service.Models.Voucher;
import com.example.java_service.Repositories.Jpa.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AppliedVoucherService
{
    AppliedVoucherRepository appliedVoucherRepository;
    UserRepository userRepository;
    VoucherRepository voucherRepository;
    BookingRepository bookingRepository;
    BookingTourRepository bookingTourRepository;
    BookingTransportTicketRepository bookingTransportRepository;
    AppliedVoucherMapper appliedVoucherMapper;

    // --- XỬ LÝ CHO BOOKING ROOM ---
    public void saveAppliedVoucher(AppliedVoucherRequest appliedVoucherRequest, UUID idBooking)
    {
        Users user = userRepository.findById(appliedVoucherRequest.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Voucher voucher = voucherRepository.findById(appliedVoucherRequest.getIdVoucher()).orElseThrow(
                () -> new ApiException(ErrorCode.VOUCHER_NOT_FOUND)
        );

        Booking booking = bookingRepository.findById(idBooking).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        AppliedVoucher appliedVoucher = AppliedVoucher.builder()
                .user(user)
                .voucher(voucher)
                .booking(booking)
                .build();
        appliedVoucherRepository.save(appliedVoucher);
    }

    public void deleteAppliedVoucher(UUID idBooking)
    {
        bookingRepository.findById(idBooking).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        appliedVoucherRepository.deleteAppliedVoucherByBooking_IdBooking(idBooking);
    }

    // --- XỬ LÝ CHO BOOKING TOUR ---
    public void saveAppliedVoucherForTour(AppliedVoucherRequest appliedVoucherRequest, UUID idBookingTour)
    {
        Users user = userRepository.findById(appliedVoucherRequest.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Voucher voucher = voucherRepository.findById(appliedVoucherRequest.getIdVoucher()).orElseThrow(
                () -> new ApiException(ErrorCode.VOUCHER_NOT_FOUND)
        );

        BookingTour bookingTour = bookingTourRepository.findById(idBookingTour).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        AppliedVoucher appliedVoucher = AppliedVoucher.builder()
                .user(user)
                .voucher(voucher)
                .bookingTour(bookingTour)
                .build();
        appliedVoucherRepository.save(appliedVoucher);
    }

    public void deleteAppliedVoucherForTour(UUID idBookingTour)
    {
        bookingTourRepository.findById(idBookingTour).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        appliedVoucherRepository.deleteAppliedVoucherByBookingTour_IdBookingTour(idBookingTour);
    }

    // --- XỬ LÝ CHO BOOKING TICKET / TRANSPORT ---
    public void saveAppliedVoucherForTicket(AppliedVoucherRequest appliedVoucherRequest, UUID idBookingTransport)
    {
        Users user = userRepository.findById(appliedVoucherRequest.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Voucher voucher = voucherRepository.findById(appliedVoucherRequest.getIdVoucher()).orElseThrow(
                () -> new ApiException(ErrorCode.VOUCHER_NOT_FOUND)
        );

        BookingTransport bookingTransport = bookingTransportRepository.findById(idBookingTransport).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        AppliedVoucher appliedVoucher = AppliedVoucher.builder()
                .user(user)
                .voucher(voucher)
                .bookingTransport(bookingTransport)
                .build();
        appliedVoucherRepository.save(appliedVoucher);
    }

    public void deleteAppliedVoucherForTicket(UUID idBookingTransport)
    {
        bookingTransportRepository.findById(idBookingTransport).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        appliedVoucherRepository.deleteAppliedVoucherByBookingTransport_IdBookingTransport(idBookingTransport);
    }
}