package com.example.java_service.Services;

import com.example.java_service.Configs.RabbitConfig;
import com.example.java_service.Constant.PredefinedApplyTo;
import com.example.java_service.Constant.PredefinedMethodPayment;
import com.example.java_service.Constant.PredefinedStatusBookingTour;
import com.example.java_service.Constant.PredefinedStatusPayment;
import com.example.java_service.Dto.Request.*;
import com.example.java_service.Dto.Response.BookingTourResponse;
import com.example.java_service.Dto.Response.NotificationCreatedEvent;
import com.example.java_service.Dto.Response.PaymentResponse;
import com.example.java_service.Dto.Response.VoucherViewResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.BookingTourMapper;
import com.example.java_service.Mappers.VoucherMapper;
import com.example.java_service.Models.*;
import com.example.java_service.Repositories.Jpa.*;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
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
import java.math.RoundingMode;
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
    VoucherRepository voucherRepository;
    UserVoucherRepository userVoucherRepository;
    UserVoucherService userVoucherService;
    AppliedVoucherService appliedVoucherService;

    AppliedVoucherRepository appliedVoucherRepository;
    VoucherMapper voucherMapper;
    ImagesRepository imagesRepository;

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
                .priceBookingTour(totalPrice)
                .pickupLocation(request.getPickupLocation())
                .languageSelected(request.getLanguageSelected())
                .specialRequests(request.getSpecialRequests())
                .status(PredefinedStatusBookingTour.STATUS_BOOKING_PENDING)
                .build();

        BookingTour savedBookingTour = bookingTourRepository.save(bookingTour);

        if (request.getUserVoucherRequests() != null && !request.getUserVoucherRequests().isEmpty()) {
            for (UserVoucherCreationRequest userVoucherRequest : request.getUserVoucherRequests()) {
                Voucher voucher = voucherRepository.findById(userVoucherRequest.getIdVoucher())
                        .orElseThrow(() -> new ApiException(ErrorCode.VOUCHER_NOT_FOUND));

                userVoucherService.saveUserVoucherForBookingTourAndTicket(userVoucherRequest, request.getIdUser());

                AppliedVoucherRequest appliedVoucherRequest = AppliedVoucherRequest.builder()
                        .idUser(request.getIdUser())
                        .idVoucher(userVoucherRequest.getIdVoucher())
                        .build();
                appliedVoucherService.saveAppliedVoucherForTour(appliedVoucherRequest, savedBookingTour.getIdBookingTour());

                if (voucher.getDiscountPercent() != null && voucher.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal targetPriceForDiscount = BigDecimal.ZERO;
                    String applyTo = voucher.getApplyTo() != null ? voucher.getApplyTo().toUpperCase() : PredefinedApplyTo.APPLY_TO_ALL;

                    switch (applyTo) {
                        case PredefinedApplyTo.APPLY_TO_TOUR:
                        case PredefinedApplyTo.APPLY_TO_ALL:
                        default:
                            targetPriceForDiscount = totalPrice;
                            break;
                    }

                    if (targetPriceForDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new ApiException(ErrorCode.VOUCHER_NOT_APPLICABLE);
                    }

                    BigDecimal discount = targetPriceForDiscount
                            .multiply(voucher.getDiscountPercent())
                            .divide(BigDecimal.valueOf(100), 10, RoundingMode.DOWN);

                    if (voucher.getMinDiscountAmount() != null && discount.compareTo(voucher.getMinDiscountAmount()) < 0) {
                        discount = voucher.getMinDiscountAmount();
                    }

                    if (voucher.getMaxDiscountAmount() != null && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                        discount = voucher.getMaxDiscountAmount();
                    }

                    if (discount.compareTo(targetPriceForDiscount) > 0) {
                        discount = targetPriceForDiscount;
                    }

                    totalPrice = totalPrice.subtract(discount);
                }
            }
        }

        savedBookingTour.setPriceBookingTour(totalPrice);
        bookingTourRepository.save(savedBookingTour);

        NotificationCreatedEvent userEvent = NotificationCreatedEvent.builder()
                .idNotification(UUID.randomUUID())
                .idUser(savedBookingTour.getUsers().getIdUser())
                .title("Thông báo đặt tour")
                .content(String.format("Thông tin đơn đặt tour %s của bạn đã được ghi nhận!", savedBookingTour.getTour().getTourName()))
                .type("booking")
                .idRef(savedBookingTour.getIdBookingTour().toString())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationCreatedEvent ownerEvent = NotificationCreatedEvent.builder()
                .idNotification(UUID.randomUUID())
                .idUser(savedBookingTour.getTour().getUsers().getIdUser())
                .title("Có người đặt tour mới")
                .content(String.format("Khách hàng %s vừa đặt tour %s của bạn.", savedBookingTour.getUsers().getLastName(), savedBookingTour.getTour().getTourName()))
                .type("booking")
                .idRef(savedBookingTour.getIdBookingTour().toString())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            rabbitTemplate.convertAndSend(rabbitConfig.EXCHANGE_NOTIFICATION, rabbitConfig.ROUTING_KEY_NOTIFICATION, objectMapper.writeValueAsString(userEvent));
            rabbitTemplate.convertAndSend(rabbitConfig.EXCHANGE_NOTIFICATION, rabbitConfig.ROUTING_KEY_NOTIFICATION, objectMapper.writeValueAsString(ownerEvent));
        } catch (Exception ex) {
            log.error("Lỗi khi gửi thông báo đặt tour: {}", ex.getMessage());
        }

        return savedBookingTour;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentResponse callPaymentApi(BookingTour bookingTour, String paymentMethod, String payerAddress) {
        try {
            if ("VNDStablecoin".equals(paymentMethod) && payerAddress != null && !payerAddress.isBlank()) {
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
                PaymentRequest paymentRequest = PaymentRequest.builder()
                        .id_user(bookingTour.getUsers().getIdUser())
                        .id_booking(null)
                        .id_booking_tour(bookingTour.getIdBookingTour())
                        .amount(bookingTour.getPriceBookingTour().doubleValue())
                        .build();

                PaymentResponse paymentResponse = webClient
                        .post()
                        .uri("/api/v1/payments/")
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

        // Lấy danh sách Voucher áp dụng
        List<AppliedVoucher> appliedVouchers = appliedVoucherRepository.findAppliedVouchersWithVoucherByBookingTourId(idBookingTour);
        List<String> voucherIds = appliedVouchers.stream()
                .map(appliedVoucher -> appliedVoucher.getVoucher().getIdVoucher().toString())
                .toList();

        List<Images> images = imagesRepository.findByRefIdIn(voucherIds);
        Map<String, String> voucherImagesMap = images.stream().collect(Collectors.toMap(Images::getRefId, Images::getUrl, (a, b) -> a));

        List<VoucherViewResponse> voucherViewResponses = appliedVouchers.stream().map(av -> {
            VoucherViewResponse voucherViewResponse = voucherMapper.toVoucherViewResponse(av.getVoucher());
            voucherViewResponse.setUrlImage(voucherImagesMap.get(av.getVoucher().getIdVoucher().toString()));
            return voucherViewResponse;
        }).toList();

        response.setVoucherViewResponses(voucherViewResponses);

        return response;
    }

    public List<BookingTourResponse> getListBookingTourByUser(UUID idUser) {
        List<BookingTour> bookings = bookingTourRepository.findByUsers_IdUser(idUser);
        return mapToResponseList(bookings);
    }

    public List<BookingTourResponse> getListBookingTourByBusiness(UUID idUser) {
        userRepository.findById(idUser).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );
        List<BookingTour> bookings = bookingTourRepository.findByTour_Users_IdUser(idUser);
        return mapToResponseList(bookings);
    }

    private List<BookingTourResponse> mapToResponseList(List<BookingTour> bookings) {
        if (bookings.isEmpty()) return List.of();

        List<UUID> bookingTourIds = bookings.stream().map(BookingTour::getIdBookingTour).collect(Collectors.toList());

        List<Payment> payments = paymentRepository.findByBookingTour_IdBookingTourIn(bookingTourIds);
        Map<UUID, Payment> paymentMap = payments.stream()
                .filter(p -> p.getBookingTour() != null)
                .collect(Collectors.toMap(p -> p.getBookingTour().getIdBookingTour(), p -> p, (existing, replacement) -> existing));

        List<AppliedVoucher> allAppliedVouchers = appliedVoucherRepository.findAppliedVouchersWithVoucherByBookingTourIds(bookingTourIds);
        Map<UUID, List<AppliedVoucher>> voucherMap = allAppliedVouchers.stream()
                .collect(Collectors.groupingBy(av -> av.getBookingTour().getIdBookingTour()));

        List<String> voucherIds = allAppliedVouchers.stream().map(av -> av.getVoucher().getIdVoucher().toString()).distinct().toList();
        List<Images> images = imagesRepository.findByRefIdIn(voucherIds);
        Map<String, String> voucherImagesMap = images.stream().collect(Collectors.toMap(Images::getRefId, Images::getUrl, (a, b) -> a));

        return bookings.stream().map(booking -> {
            BookingTourResponse response = bookingTourMapper.toBookingTourResponse(booking);
            Payment payment = paymentMap.get(booking.getIdBookingTour());
            if (payment != null) {
                response.setPaymentInBookingResponse(bookingTourMapper.toPaymentInBookingResponse(payment));
            }

            List<AppliedVoucher> avs = voucherMap.getOrDefault(booking.getIdBookingTour(), List.of());
            List<VoucherViewResponse> vResList = avs.stream().map(av -> {
                VoucherViewResponse vRes = voucherMapper.toVoucherViewResponse(av.getVoucher());
                vRes.setUrlImage(voucherImagesMap.get(av.getVoucher().getIdVoucher().toString()));
                return vRes;
            }).toList();
            response.setVoucherViewResponses(vResList);

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
            bookingTourMapper.updateBookingTourFromRequest(request, bookingTour);

            int qtyAdult = bookingTour.getQuantityAdult() != null ? bookingTour.getQuantityAdult() : 1;
            int qtyChild = bookingTour.getQuantityChild() != null ? bookingTour.getQuantityChild() : 0;

            BigDecimal adultPrice = bookingTour.getTour().getAdultPrice() != null ? bookingTour.getTour().getAdultPrice() : BigDecimal.ZERO;
            BigDecimal childPrice = bookingTour.getTour().getChildPrice() != null ? bookingTour.getTour().getChildPrice() : BigDecimal.ZERO;

            BigDecimal totalPrice = adultPrice.multiply(BigDecimal.valueOf(qtyAdult)).add(childPrice.multiply(BigDecimal.valueOf(qtyChild)));

            if (request.getUserVoucherRequests() != null && !request.getUserVoucherRequests().isEmpty()) {

                appliedVoucherService.deleteAppliedVoucherForTour(bookingTour.getIdBookingTour());

                List<UserVoucher> usedVouchers = userVoucherRepository.findUsedVouchersByBookingTourId(bookingTour.getIdBookingTour());
                usedVouchers.forEach(uv -> {
                    uv.setIsUsed(false);
                    uv.setUsedAt(null);
                });
                userVoucherRepository.saveAll(usedVouchers);

                for (UserVoucherCreationRequest userVoucherRequest : request.getUserVoucherRequests()) {
                    Voucher voucher = voucherRepository.findById(userVoucherRequest.getIdVoucher())
                            .orElseThrow(() -> new ApiException(ErrorCode.VOUCHER_NOT_FOUND));

                    userVoucherService.saveUserVoucherForBookingTourAndTicket(userVoucherRequest, request.getIdUser());

                    AppliedVoucherRequest appliedVoucherRequest = AppliedVoucherRequest.builder()
                            .idUser(request.getIdUser())
                            .idVoucher(userVoucherRequest.getIdVoucher())
                            .build();
                    appliedVoucherService.saveAppliedVoucherForTour(appliedVoucherRequest, bookingTour.getIdBookingTour());

                    if (voucher.getDiscountPercent() != null && voucher.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal targetPriceForDiscount = BigDecimal.ZERO;
                        String applyTo = voucher.getApplyTo() != null ? voucher.getApplyTo().toUpperCase() : PredefinedApplyTo.APPLY_TO_ALL;

                        switch (applyTo) {
                            case PredefinedApplyTo.APPLY_TO_TOUR:
                            case PredefinedApplyTo.APPLY_TO_ALL:
                            default:
                                targetPriceForDiscount = totalPrice;
                                break;
                        }

                        if (targetPriceForDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                            throw new ApiException(ErrorCode.VOUCHER_NOT_APPLICABLE);
                        }

                        BigDecimal discount = targetPriceForDiscount
                                .multiply(voucher.getDiscountPercent())
                                .divide(BigDecimal.valueOf(100), 10, RoundingMode.DOWN);

                        if (voucher.getMinDiscountAmount() != null && discount.compareTo(voucher.getMinDiscountAmount()) < 0) {
                            discount = voucher.getMinDiscountAmount();
                        }

                        if (voucher.getMaxDiscountAmount() != null && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                            discount = voucher.getMaxDiscountAmount();
                        }

                        if (discount.compareTo(targetPriceForDiscount) > 0) {
                            discount = targetPriceForDiscount;
                        }

                        totalPrice = totalPrice.subtract(discount);
                    }
                }
            }

            bookingTour.setPriceBookingTour(totalPrice);
            bookingTourRepository.save(bookingTour);

            Payment payment = paymentRepository.findByBookingTour_IdBookingTour(idBookingTour);
            if (payment != null) {
                payment.setStatus(PredefinedStatusPayment.STATUS_PAYMENT_FAILED);
                paymentRepository.save(payment);
            }

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