package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedMethodPayment;
import com.example.java_service.Constant.PredefinedStatusPayment;
import com.example.java_service.Dto.Request.BookingTourCreationRequest;
import com.example.java_service.Dto.Request.BookingTourUpdateRequest;
import com.example.java_service.Dto.Request.PaymentRequest;
import com.example.java_service.Dto.Response.BookingTourResponse;
import com.example.java_service.Dto.Response.PaymentResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.BookingTourMapper;
import com.example.java_service.Models.BookingTour;
import com.example.java_service.Models.Payment;
import com.example.java_service.Models.Tour;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.BookingTourRepository;
import com.example.java_service.Repositories.Jpa.PaymentRepository;
import com.example.java_service.Repositories.Jpa.TourRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingTourService {

    BookingTourRepository bookingTourRepository;
    TourRepository tourRepository;
    UserRepository userRepository;
    PaymentRepository paymentRepository;
    BookingTourMapper bookingTourMapper;
    WebClient webClient;

    @NonFinal
    BookingTourService self;

    @Autowired
    @Lazy
    public void setSelf(BookingTourService self) {
        this.self = self;
    }

    public PaymentResponse createBookingTour(BookingTourCreationRequest request, String paymentMethod, String payerAddress) {
        BookingTour savedBookingTour = self.createBookingTourTransactional(request);

        return self.callPaymentApi(savedBookingTour, paymentMethod, payerAddress);
    }

    @Transactional(rollbackFor = Exception.class)
    public BookingTour createBookingTourTransactional(BookingTourCreationRequest request) {
        Users user = userRepository.findById(request.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Tour tour = tourRepository.findById(request.getIdTour()).orElseThrow(
                () -> new ApiException(ErrorCode.TOUR_NOT_FOUND)
        );

        int qtyAdult = request.getQuantityAdult() != null ? request.getQuantityAdult() : 1;
        int qtyChild = request.getQuantityChild() != null ? request.getQuantityChild() : 0;

        BigDecimal adultPrice = tour.getAdultPrice() != null ? tour.getAdultPrice() : BigDecimal.ZERO;
        BigDecimal childPrice = tour.getChildPrice() != null ? tour.getChildPrice() : BigDecimal.ZERO;

        BigDecimal totalAdultPrice = adultPrice.multiply(BigDecimal.valueOf(qtyAdult));
        BigDecimal totalChildPrice = childPrice.multiply(BigDecimal.valueOf(qtyChild));
        BigDecimal totalPrice = totalAdultPrice.add(totalChildPrice);

        BookingTour bookingTour = BookingTour.builder()
                .users(user)
                .tour(tour)
                .quantityAdult(qtyAdult)
                .quantityChild(qtyChild)
                .priceBookingTour(totalPrice) // Lưu tổng tiền
                .pickupLocation(request.getPickupLocation())
                .languageSelected(request.getLanguageSelected())
                .specialRequests(request.getSpecialRequests())
                .build();

        return bookingTourRepository.save(bookingTour);
    }

    // Gọi API Thanh toán
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentResponse callPaymentApi(BookingTour bookingTour, String paymentMethod, String payerAddress) {
        try {
            if ("VNDStablecoin".equals(paymentMethod) && payerAddress != null && !payerAddress.isBlank()) {
                // Flow thanh toán bằng Crypto/Stablecoin
                Map<String, Object> vndRequest = Map.of(
                        "id_user", bookingTour.getUsers().getIdUser(),
                        "id_booking_tour", bookingTour.getIdBookingTour(),
                        "amount", bookingTour.getPriceBookingTour().doubleValue(),
                        "payer_address", payerAddress
                );

                @SuppressWarnings("unchecked")
                Map<String, Object> vndResponse = webClient
                        .post()
                        .uri("/api/v1/payments/vnd/prepare")
                        .header("Content-Type", "application/json")
                        .bodyValue(vndRequest)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                if (vndResponse == null) {
                    throw new ApiException(ErrorCode.PAYMENT_FAILED);
                }

                return PaymentResponse.builder()
                        .idPayment(vndResponse.get("id_payment") != null
                                ? UUID.fromString(vndResponse.get("id_payment").toString())
                                : null)
                        .amount(bookingTour.getPriceBookingTour())
                        .method("VNDStablecoin")
                        .status("Pending")
                        .typedData((Map<String, Object>) vndResponse.get("typed_data"))
                        .orderId(vndResponse.get("order_id") != null ? vndResponse.get("order_id").toString() : null)
                        .build();
            } else {
                // Flow thanh toán mặc định (ZaloPay, v.v...)
                PaymentRequest paymentRequest = PaymentRequest.builder()
                        .id_user(bookingTour.getUsers().getIdUser())
                        .id_booking(null)
                        .id_booking_tour(bookingTour.getIdBookingTour())
                        .amount(bookingTour.getPriceBookingTour().doubleValue())
                        .build();

                PaymentResponse paymentResponse = webClient
                        .post()
                        .uri("/api/v1/payments")
                        .header("Content-Type", "application/json")
                        .bodyValue(paymentRequest)
                        .retrieve()
                        .bodyToMono(PaymentResponse.class)
                        .block();

                if (paymentResponse == null) {
                    throw new ApiException(ErrorCode.PAYMENT_FAILED);
                }

                return paymentResponse;
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi gọi API thanh toán Tour: {}", e.getMessage(), e);
            throw new ApiException(ErrorCode.BOOKING_FAILED);
        }
    }

    public BookingTourResponse getBookingTourDetail(UUID idBookingTour) {
        BookingTour bookingTour = bookingTourRepository.findById(idBookingTour).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );
        return bookingTourMapper.toBookingTourResponse(bookingTour);
    }

    public List<BookingTourResponse> getListBookingTourByUser(UUID idUser) {
        List<BookingTour> bookings = bookingTourRepository.findByUsers_IdUser(idUser);
        return bookings.stream()
                .map(bookingTourMapper::toBookingTourResponse)
                .collect(Collectors.toList());
    }

    public List<BookingTourResponse> getListBookingTourByBusiness(UUID idUser) {
        userRepository.findById(idUser).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        List<BookingTour> bookings = bookingTourRepository.findByTour_Users_IdUser(idUser);

        return bookings.stream()
                .map(bookingTourMapper::toBookingTourResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponse updateBookingTour(UUID idBookingTour, BookingTourUpdateRequest request) {
        if (paymentRepository.existsByBookingTour_IdBookingTourAndStatus(idBookingTour, PredefinedStatusPayment.STATUS_PAYMENT_SUCCESS)) {
            throw new ApiException(ErrorCode.PAID);
        }

        BookingTour bookingTour = bookingTourRepository.findById(idBookingTour).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        Payment payment = paymentRepository.findByBookingTour_IdBookingTour(idBookingTour);
        if (payment != null) {
            payment.setStatus(PredefinedStatusPayment.STATUS_PAYMENT_FAILED);
            paymentRepository.save(payment);
        }

        String method = request.getPaymentMethod() != null ? request.getPaymentMethod() : PredefinedMethodPayment.ZALOPAY_METHOD;
        return self.callPaymentApi(bookingTour, method, request.getPayerAddress());
    }

    @Transactional
    public String cancelBookingTour(UUID idBookingTour) {
        if (paymentRepository.existsByBookingTour_IdBookingTourAndStatus(idBookingTour, PredefinedStatusPayment.STATUS_PAYMENT_SUCCESS)) {
            throw new ApiException(ErrorCode.PAID);
        }

        bookingTourRepository.findById(idBookingTour).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        Payment payment = paymentRepository.findByBookingTour_IdBookingTour(idBookingTour);
        if (payment != null) {
            payment.setStatus(PredefinedStatusPayment.STATUS_PAYMENT_FAILED);
            paymentRepository.save(payment);
        }

        return "Hủy đặt tour thành công!";
    }
}