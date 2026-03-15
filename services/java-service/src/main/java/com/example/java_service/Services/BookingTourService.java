package com.example.java_service.Services;

import com.example.java_service.Configs.RabbitConfig;
import com.example.java_service.Constant.PredefinedMethodPayment;
import com.example.java_service.Constant.PredefinedStatusBookingTour;
import com.example.java_service.Constant.PredefinedStatusPayment;
import com.example.java_service.Dto.Request.BookingTourCreationRequest;
import com.example.java_service.Dto.Request.BookingTourUpdateRequest;
import com.example.java_service.Dto.Request.PaymentRequest;
import com.example.java_service.Dto.Response.BookingTourResponse;
import com.example.java_service.Dto.Response.NotificationCreatedEvent;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    RabbitTemplate rabbitTemplate;
    RabbitConfig rabbitConfig;
    ObjectMapper objectMapper;
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
                .status(PredefinedStatusBookingTour.STATUS_BOOKING_PENDING)
                .build();

        BookingTour savedBookingTour = bookingTourRepository.save(bookingTour);

        // --- Gửi thông báo cho người đặt tour ---
        NotificationCreatedEvent userEvent = NotificationCreatedEvent.builder()
                .idNotification(UUID.randomUUID())
                .idUser(savedBookingTour.getUsers().getIdUser())
                .title("Thông báo đặt tour")
                .content(String.format("Thông tin đơn đặt tour %s của bạn đã được ghi nhận!", savedBookingTour.getTour().getTourName())) // Giả sử model Tour có getTourName()
                .type("booking")
                .idRef(savedBookingTour.getIdBookingTour().toString())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        // --- Gửi thông báo cho người chủ tour ---
        NotificationCreatedEvent ownerEvent = NotificationCreatedEvent.builder()
                .idNotification(UUID.randomUUID())
                .idUser(savedBookingTour.getTour().getUsers().getIdUser()) // Chủ sở hữu tour
                .title("Có người đặt tour mới")
                .content(String.format("Khách hàng %s vừa đặt tour %s của bạn.", savedBookingTour.getUsers().getLastName(), savedBookingTour.getTour().getTourName()))
                .type("booking")
                .idRef(savedBookingTour.getIdBookingTour().toString())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            // Gửi cho user
            rabbitTemplate.convertAndSend(
                    rabbitConfig.EXCHANGE_NOTIFICATION,
                    rabbitConfig.ROUTING_KEY_NOTIFICATION,
                    objectMapper.writeValueAsString(userEvent)
            );

            // Gửi cho owner
            rabbitTemplate.convertAndSend(
                    rabbitConfig.EXCHANGE_NOTIFICATION,
                    rabbitConfig.ROUTING_KEY_NOTIFICATION,
                    objectMapper.writeValueAsString(ownerEvent)
            );
        } catch (Exception ex) {
            log.error("Lỗi khi gửi thông báo đặt tour: {}", ex.getMessage());
        }

        return savedBookingTour;
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

        BookingTourResponse response = bookingTourMapper.toBookingTourResponse(bookingTour);

        Payment payment = paymentRepository.findByBookingTour_IdBookingTour(idBookingTour);
        if (payment != null) {
            response.setPaymentInBookingResponse(bookingTourMapper.toPaymentInBookingResponse(payment));
        }

        return response;
    }

    public List<BookingTourResponse> getListBookingTourByUser(UUID idUser) {
        List<BookingTour> bookings = bookingTourRepository.findByUsers_IdUser(idUser);

        if (bookings.isEmpty()) {
            return List.of();
        }

        // Lấy danh sách ID để query Payment 1 lần
        List<UUID> bookingTourIds = bookings.stream()
                .map(BookingTour::getIdBookingTour)
                .collect(Collectors.toList());

        List<Payment> payments = paymentRepository.findByBookingTour_IdBookingTourIn(bookingTourIds);

        Map<UUID, Payment> paymentMap = payments.stream()
                .filter(p -> p.getBookingTour() != null)
                .collect(Collectors.toMap(
                        p -> p.getBookingTour().getIdBookingTour(),
                        p -> p,
                        (existing, replacement) -> existing
                ));

        return bookings.stream().map(booking -> {
            BookingTourResponse response = bookingTourMapper.toBookingTourResponse(booking);

            Payment payment = paymentMap.get(booking.getIdBookingTour());
            if (payment != null) {
                response.setPaymentInBookingResponse(bookingTourMapper.toPaymentInBookingResponse(payment));
            }

            return response;
        }).collect(Collectors.toList());
    }

    public List<BookingTourResponse> getListBookingTourByBusiness(UUID idUser) {
        userRepository.findById(idUser).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        List<BookingTour> bookings = bookingTourRepository.findByTour_Users_IdUser(idUser);

        if (bookings.isEmpty()) {
            return List.of();
        }

        List<UUID> bookingTourIds = bookings.stream()
                .map(BookingTour::getIdBookingTour)
                .collect(Collectors.toList());

        List<Payment> payments = paymentRepository.findByBookingTour_IdBookingTourIn(bookingTourIds);

        Map<UUID, Payment> paymentMap = payments.stream()
                .filter(p -> p.getBookingTour() != null)
                .collect(Collectors.toMap(
                        p -> p.getBookingTour().getIdBookingTour(),
                        p -> p,
                        (existing, replacement) -> existing
                ));

        return bookings.stream().map(booking -> {
            BookingTourResponse response = bookingTourMapper.toBookingTourResponse(booking);

            Payment payment = paymentMap.get(booking.getIdBookingTour());
            if (payment != null) {
                response.setPaymentInBookingResponse(bookingTourMapper.toPaymentInBookingResponse(payment));
            }

            return response;
        }).collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponse updateBookingTour(UUID idBookingTour, BookingTourUpdateRequest request) {
        if (paymentRepository.existsByBookingTour_IdBookingTourAndStatus(idBookingTour, PredefinedStatusPayment.STATUS_PAYMENT_SUCCESS)) {
            throw new ApiException(ErrorCode.PAID);
        }

        BookingTour bookingTour = bookingTourRepository.findById(idBookingTour).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        boolean isBusinessUpdatingStatus = request.getStatus() != null && !request.getStatus().isBlank();

        if (isBusinessUpdatingStatus) {
            // TRƯỜNG HỢP 1: BUSINESS CẬP NHẬT TRẠNG THÁI (DUYỆT/TỪ CHỐI)
            List<String> validStatuses = List.of(
                    PredefinedStatusBookingTour.STATUS_BOOKING_PENDING,
                    PredefinedStatusBookingTour.STATUS_BOOKING_SUCCESS,
                    PredefinedStatusBookingTour.STATUS_BOOKING_FAILED
            );

            if (!validStatuses.contains(request.getStatus())) {
                throw new ApiException(ErrorCode.INVALID_STATUS_BOOKING_TOUR);
            }

            bookingTour.setStatus(request.getStatus());
            bookingTourRepository.save(bookingTour);

            // Chỉ gửi thông báo cho User: Trạng thái đơn của họ đã thay đổi
            NotificationCreatedEvent userEvent = NotificationCreatedEvent.builder()
                    .idNotification(UUID.randomUUID())
                    .idUser(bookingTour.getUsers().getIdUser())
                    .title("Cập nhật trạng thái đặt tour")
                    .content(String.format("Đơn đặt tour %s của bạn đã được chuyển sang trạng thái: %s", bookingTour.getTour().getTourName(), request.getStatus()))
                    .type("booking")
                    .idRef(bookingTour.getIdBookingTour().toString())
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            try {
                rabbitTemplate.convertAndSend(rabbitConfig.EXCHANGE_NOTIFICATION, rabbitConfig.ROUTING_KEY_NOTIFICATION, objectMapper.writeValueAsString(userEvent));
            } catch (Exception ex) {
                log.error("Lỗi khi gửi thông báo cập nhật trạng thái tour cho User: {}", ex.getMessage());
            }

            return PaymentResponse.builder()
                    .status(request.getStatus())
                    .build();

        } else {
            // TRƯỜNG HỢP 2: USER SỬA THÔNG TIN ĐỂ THANH TOÁN LẠI
            bookingTourMapper.updateBookingTourFromRequest(request, bookingTour);
            bookingTourRepository.save(bookingTour);

            Payment payment = paymentRepository.findByBookingTour_IdBookingTour(idBookingTour);
            if (payment != null) {
                payment.setStatus(PredefinedStatusPayment.STATUS_PAYMENT_FAILED);
                paymentRepository.save(payment);
            }

            // Chỉ gửi thông báo cho Chủ Tour (Business): User đã thay đổi thông tin
            NotificationCreatedEvent ownerEvent = NotificationCreatedEvent.builder()
                    .idNotification(UUID.randomUUID())
                    .idUser(bookingTour.getTour().getUsers().getIdUser())
                    .title("Khách hàng cập nhật đơn đặt tour")
                    .content(String.format("Khách hàng %s đã cập nhật thông tin hoặc đang thực hiện thanh toán lại cho đơn đặt tour %s.", bookingTour.getUsers().getLastName(), bookingTour.getTour().getTourName()))
                    .type("booking")
                    .idRef(bookingTour.getIdBookingTour().toString())
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            try {
                rabbitTemplate.convertAndSend(rabbitConfig.EXCHANGE_NOTIFICATION, rabbitConfig.ROUTING_KEY_NOTIFICATION, objectMapper.writeValueAsString(ownerEvent));
            } catch (Exception ex) {
                log.error("Lỗi khi gửi thông báo user cập nhật tour cho Business: {}", ex.getMessage());
            }

            String method = request.getPaymentMethod() != null ? request.getPaymentMethod() : PredefinedMethodPayment.ZALOPAY_METHOD;
            return self.callPaymentApi(bookingTour, method, request.getPayerAddress());
        }
    }

    @Transactional
    public String cancelBookingTour(UUID idBookingTour) {
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

        bookingTour.setStatus(PredefinedStatusBookingTour.STATUS_BOOKING_FAILED);
        bookingTourRepository.save(bookingTour);

        // --- Thông báo cho người chủ tour ---
        NotificationCreatedEvent ownerEvent = NotificationCreatedEvent.builder()
                .idNotification(UUID.randomUUID())
                .idUser(bookingTour.getTour().getUsers().getIdUser())
                .title("Thông báo hủy đơn đặt tour")
                .content(String.format("Khách hàng %s đã hủy đơn đặt tour %s.", bookingTour.getUsers().getLastName(), bookingTour.getTour().getTourName()))
                .type("booking_tour")
                .idRef(bookingTour.getIdBookingTour().toString())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            rabbitTemplate.convertAndSend(
                    rabbitConfig.EXCHANGE_NOTIFICATION,
                    rabbitConfig.ROUTING_KEY_NOTIFICATION,
                    objectMapper.writeValueAsString(ownerEvent)
            );
        } catch (Exception ex) {
            log.error("Lỗi khi gửi thông báo hủy đặt tour: {}", ex.getMessage());
        }

        return "Hủy đặt tour thành công!";
    }
}