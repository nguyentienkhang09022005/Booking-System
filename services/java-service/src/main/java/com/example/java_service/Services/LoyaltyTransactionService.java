package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedLoyaltyPoint;
import com.example.java_service.Dto.Request.AddPointRequest;
import com.example.java_service.Dto.Response.LoyaltyTransactionResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.LoyaltyTransactionMapper;
import com.example.java_service.Models.*;
import com.example.java_service.Repositories.Jpa.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoyaltyTransactionService {

    LoyaltyTransactionRepository loyaltyTransactionRepository;
    UserRepository userRepository;
    BookingRepository bookingRepository;
    BookingTourRepository bookingTourRepository;
    BookingTransportTicketRepository bookingTransportRepository;
    LoyaltyTransactionMapper mapper;

    @Transactional
    public void processLoyaltyPoints(AddPointRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Users user = userRepository.findById(request.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        int pointsToAdd = request.getAmount().divide(PredefinedLoyaltyPoint.VND_PER_POINT, 0, RoundingMode.DOWN).intValue();

        if (pointsToAdd > 0) {
            int currentPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0;
            user.setLoyaltyPoints(currentPoints + pointsToAdd);
            userRepository.save(user);

            LoyaltyTransaction.LoyaltyTransactionBuilder builder = LoyaltyTransaction.builder()
                    .users(user)
                    .points(pointsToAdd)
                    .description("Tích điểm từ giao dịch thanh toán thành công (" + request.getAmount() + " VNĐ)");

            if (request.getIdBooking() != null) {
                Booking booking = bookingRepository.findById(request.getIdBooking())
                        .orElseThrow(() -> new ApiException(ErrorCode.BOOKING_NOT_FOUND));
                builder.booking(booking);
            } else if (request.getIdBookingTour() != null) {
                BookingTour tour = bookingTourRepository.findById(request.getIdBookingTour())
                        .orElseThrow(() -> new ApiException(ErrorCode.BOOKING_NOT_FOUND));
                builder.bookingTour(tour);
            } else if (request.getIdBookingTransport() != null) {
                BookingTransport transport = bookingTransportRepository.findById(request.getIdBookingTransport())
                        .orElseThrow(() -> new ApiException(ErrorCode.BOOKING_NOT_FOUND));
                builder.bookingTransport(transport);
            }

            loyaltyTransactionRepository.save(builder.build());
            log.info("Đã cộng {} Loyalty Points cho user {}. Giao dịch: {} VNĐ!", pointsToAdd, request.getIdUser(), request.getAmount());
        }
    }

    public List<LoyaltyTransactionResponse> getHistoryByUser(UUID idUser) {
        userRepository.findById(idUser).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return loyaltyTransactionRepository.findByUsers_IdUserOrderByCreatedAtDesc(idUser)
                .stream().map(mapper::toResponse).collect(Collectors.toList());
    }

    public List<LoyaltyTransactionResponse> getRoomBookingHistoryByUser(UUID idUser) {
        userRepository.findById(idUser).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return loyaltyTransactionRepository.findByUsers_IdUserAndBookingIsNotNullOrderByCreatedAtDesc(idUser)
                .stream().map(mapper::toResponse).collect(Collectors.toList());
    }

    public List<LoyaltyTransactionResponse> getTourBookingHistoryByUser(UUID idUser) {
        userRepository.findById(idUser).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return loyaltyTransactionRepository.findByUsers_IdUserAndBookingTourIsNotNullOrderByCreatedAtDesc(idUser)
                .stream().map(mapper::toResponse).collect(Collectors.toList());
    }

    public List<LoyaltyTransactionResponse> getTransportBookingHistoryByUser(UUID idUser) {
        userRepository.findById(idUser).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return loyaltyTransactionRepository.findByUsers_IdUserAndBookingTransportIsNotNullOrderByCreatedAtDesc(idUser)
                .stream().map(mapper::toResponse).collect(Collectors.toList());
    }
}