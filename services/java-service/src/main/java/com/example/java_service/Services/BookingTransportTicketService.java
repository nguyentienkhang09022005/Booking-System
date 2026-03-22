package com.example.java_service.Services;

import com.example.java_service.Configs.RabbitConfig;
import com.example.java_service.Constant.PredefinedMethodPayment;
import com.example.java_service.Constant.PredefinedStatusBookingTransport;
import com.example.java_service.Constant.PredefinedStatusPayment;
import com.example.java_service.Dto.Request.BookingTransportCreationRequest;
import com.example.java_service.Dto.Request.BookingTransportUpdateRequest;
import com.example.java_service.Dto.Request.PaymentRequest;
import com.example.java_service.Dto.Response.BookingTransportResponse;
import com.example.java_service.Dto.Response.NotificationCreatedEvent;
import com.example.java_service.Dto.Response.PaymentResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.BookingTransportTicketMapper;
import com.example.java_service.Models.BookingTransport;
import com.example.java_service.Models.Payment;
import com.example.java_service.Models.TransportTicket;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.BookingTransportTicketRepository;
import com.example.java_service.Repositories.Jpa.PaymentRepository;
import com.example.java_service.Repositories.Jpa.TransportTicketRepository;
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
public class BookingTransportTicketService {

    BookingTransportTicketRepository bookingTransportRepository;
    TransportTicketRepository transportTicketRepository;
    UserRepository userRepository;
    PaymentRepository paymentRepository;
    BookingTransportTicketMapper bookingTransportMapper;
    RabbitTemplate rabbitTemplate;
    RabbitConfig rabbitConfig;
    ObjectMapper objectMapper;
    WebClient webClient;

    @NonFinal
    BookingTransportTicketService self;

    @Autowired
    @Lazy
    public void setSelf(BookingTransportTicketService self) {
        this.self = self;
    }

    public PaymentResponse createBookingTransport(BookingTransportCreationRequest request, String paymentMethod, String payerAddress) {
        BookingTransport savedBooking = self.createBookingTransportTransactional(request);
        return self.callPaymentApi(savedBooking, paymentMethod, payerAddress);
    }

    @Transactional(rollbackFor = Exception.class)
    public BookingTransport createBookingTransportTransactional(BookingTransportCreationRequest request) {
        Users user = userRepository.findById(request.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        TransportTicket ticket = transportTicketRepository.findById(request.getIdTicket()).orElseThrow(
                () -> new ApiException(ErrorCode.TRANSPORT_TICKET_NOT_FOUND)
        );

        int qtyTickets = request.getQuantityTickets() != null ? request.getQuantityTickets() : 1;
        if (qtyTickets <= 0) {
            throw new ApiException(ErrorCode.QUANTITY_NOT_ENOUGH);
        }

        BigDecimal ticketPrice = ticket.getPrice() != null ? ticket.getPrice() : BigDecimal.ZERO;
        BigDecimal totalPrice = ticketPrice.multiply(BigDecimal.valueOf(qtyTickets));

        BookingTransport bookingTransport = BookingTransport.builder()
                .users(user)
                .transportTicket(ticket)
                .quantityTickets(qtyTickets)
                .priceBookingTransport(totalPrice)
                .seatNumbers(request.getSeatNumbers())
                .status(PredefinedStatusBookingTransport.STATUS_BOOKING_PENDING)
                .build();

        BookingTransport savedBooking = bookingTransportRepository.save(bookingTransport);

        // --- Gửi thông báo cho người đặt vé ---
        NotificationCreatedEvent userEvent = NotificationCreatedEvent.builder()
                .idNotification(UUID.randomUUID())
                .idUser(savedBooking.getUsers().getIdUser())
                .title("Thông báo đặt vé di chuyển")
                .content("Thông tin đơn đặt vé của bạn đã được ghi nhận!")
                .type("booking")
                .idRef(savedBooking.getIdBookingTransport().toString())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        // --- Gửi thông báo cho người chủ vé (Business) ---
        NotificationCreatedEvent ownerEvent = NotificationCreatedEvent.builder()
                .idNotification(UUID.randomUUID())
                .idUser(savedBooking.getTransportTicket().getUsers().getIdUser())
                .title("Có người đặt vé mới")
                .content(String.format("Khách hàng %s vừa đặt %d vé.", savedBooking.getUsers().getLastName(), qtyTickets))
                .type("booking")
                .idRef(savedBooking.getIdBookingTransport().toString())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            rabbitTemplate.convertAndSend(rabbitConfig.EXCHANGE_NOTIFICATION, rabbitConfig.ROUTING_KEY_NOTIFICATION, objectMapper.writeValueAsString(userEvent));
            rabbitTemplate.convertAndSend(rabbitConfig.EXCHANGE_NOTIFICATION, rabbitConfig.ROUTING_KEY_NOTIFICATION, objectMapper.writeValueAsString(ownerEvent));
        } catch (Exception ex) {
            log.error("Lỗi khi gửi thông báo đặt vé di chuyển: {}", ex.getMessage());
        }

        return savedBooking;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentResponse callPaymentApi(BookingTransport bookingTransport, String paymentMethod, String payerAddress) {
        try {
            if ("VNDStablecoin".equals(paymentMethod) && payerAddress != null && !payerAddress.isBlank()) {
                Map<String, Object> vndRequest = Map.of(
                        "id_user", bookingTransport.getUsers().getIdUser(),
                        "id_booking_transport", bookingTransport.getIdBookingTransport(),
                        "amount", bookingTransport.getPriceBookingTransport().doubleValue(),
                        "payer_address", payerAddress
                );

                @SuppressWarnings("unchecked")
                Map<String, Object> vndResponse = webClient.post()
                        .uri("/api/v1/payments/vnd/prepare")
                        .header("Content-Type", "application/json")
                        .bodyValue(vndRequest)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                if (vndResponse == null) throw new ApiException(ErrorCode.PAYMENT_FAILED);

                return PaymentResponse.builder()
                        .idPayment(vndResponse.get("id_payment") != null ? UUID.fromString(vndResponse.get("id_payment").toString()) : null)
                        .amount(bookingTransport.getPriceBookingTransport())
                        .method("VNDStablecoin")
                        .status("Pending")
                        .typedData((Map<String, Object>) vndResponse.get("typed_data"))
                        .orderId(vndResponse.get("order_id") != null ? vndResponse.get("order_id").toString() : null)
                        .build();
            } else {
                PaymentRequest paymentRequest = PaymentRequest.builder()
                        .id_user(bookingTransport.getUsers().getIdUser())
                        .id_booking_transport(bookingTransport.getIdBookingTransport())
                        .amount(bookingTransport.getPriceBookingTransport().doubleValue())
                        .build();

                PaymentResponse paymentResponse = webClient.post()
                        .uri("/api/v1/payments/")
                        .header("Content-Type", "application/json")
                        .bodyValue(paymentRequest)
                        .retrieve()
                        .bodyToMono(PaymentResponse.class)
                        .block();

                if (paymentResponse == null) throw new ApiException(ErrorCode.PAYMENT_FAILED);

                return paymentResponse;
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi gọi API thanh toán vé di chuyển: {}", e.getMessage(), e);
            throw new ApiException(ErrorCode.BOOKING_FAILED);
        }
    }

    public BookingTransportResponse getBookingDetail(UUID idBookingTransport) {
        BookingTransport booking = bookingTransportRepository.findById(idBookingTransport).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        BookingTransportResponse response = bookingTransportMapper.toBookingTransportResponse(booking);
        Payment payment = paymentRepository.findByBookingTransport_IdBookingTransport(idBookingTransport);
        if (payment != null) {
            response.setPaymentInBookingResponse(bookingTransportMapper.toPaymentInBookingResponse(payment));
        }

        return response;
    }

    public List<BookingTransportResponse> getListBookingByUser(UUID idUser) {
        List<BookingTransport> bookings = bookingTransportRepository.findByUsers_IdUser(idUser);
        return mapToResponseList(bookings);
    }

    public List<BookingTransportResponse> getListBookingByBusiness(UUID idUser) {
        userRepository.findById(idUser).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        List<BookingTransport> bookings = bookingTransportRepository.findByTransportTicket_Users_IdUser(idUser);
        return mapToResponseList(bookings);
    }

    private List<BookingTransportResponse> mapToResponseList(List<BookingTransport> bookings) {
        if (bookings.isEmpty()) return List.of();

        List<UUID> bookingIds = bookings.stream().map(BookingTransport::getIdBookingTransport).collect(Collectors.toList());
        List<Payment> payments = paymentRepository.findByBookingTransport_IdBookingTransportIn(bookingIds);

        Map<UUID, Payment> paymentMap = payments.stream()
                .filter(p -> p.getBookingTransport() != null)
                .collect(Collectors.toMap(p -> p.getBookingTransport().getIdBookingTransport(), p -> p, (existing, replacement) -> existing));

        return bookings.stream().map(booking -> {
            BookingTransportResponse response = bookingTransportMapper.toBookingTransportResponse(booking);
            Payment payment = paymentMap.get(booking.getIdBookingTransport());
            if (payment != null) {
                response.setPaymentInBookingResponse(bookingTransportMapper.toPaymentInBookingResponse(payment));
            }
            return response;
        }).collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponse updateBookingTransport(UUID idBookingTransport, BookingTransportUpdateRequest request) {
        if (paymentRepository.existsByBookingTransport_IdBookingTransportAndStatus(idBookingTransport, PredefinedStatusPayment.STATUS_PAYMENT_SUCCESS)) {
            throw new ApiException(ErrorCode.PAID);
        }

        BookingTransport booking = bookingTransportRepository.findById(idBookingTransport).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        boolean isBusinessUpdatingStatus = request.getStatus() != null && !request.getStatus().isBlank();

        if (isBusinessUpdatingStatus) {
            // TRƯỜNG HỢP 1: BUSINESS CẬP NHẬT TRẠNG THÁI (DUYỆT/TỪ CHỐI)
            List<String> validStatuses = List.of(
                    PredefinedStatusBookingTransport.STATUS_BOOKING_PENDING,
                    PredefinedStatusBookingTransport.STATUS_BOOKING_SUCCESS,
                    PredefinedStatusBookingTransport.STATUS_BOOKING_FAILED
            );

            if (!validStatuses.contains(request.getStatus())) {
                throw new ApiException(ErrorCode.INVALID_DATA);
            }

            booking.setStatus(request.getStatus());
            bookingTransportRepository.save(booking);

            // Chỉ gửi thông báo cho User: Trạng thái đơn đặt vé của họ đã thay đổi
            NotificationCreatedEvent userEvent = NotificationCreatedEvent.builder()
                    .idNotification(UUID.randomUUID())
                    .idUser(booking.getUsers().getIdUser())
                    .title("Cập nhật trạng thái đặt vé")
                    .content(String.format("Đơn đặt vé di chuyển (%s) của bạn đã được chuyển sang trạng thái: %s", booking.getTransportTicket().getTicketType(), request.getStatus()))
                    .type("booking")
                    .idRef(booking.getIdBookingTransport().toString())
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            try {
                rabbitTemplate.convertAndSend(rabbitConfig.EXCHANGE_NOTIFICATION, rabbitConfig.ROUTING_KEY_NOTIFICATION, objectMapper.writeValueAsString(userEvent));
            } catch (Exception ex) {
                log.error("Lỗi khi gửi thông báo cập nhật trạng thái vé cho User: {}", ex.getMessage());
            }

            return PaymentResponse.builder()
                    .status(request.getStatus())
                    .build();

        } else {
            // TRƯỜNG HỢP 2: USER SỬA THÔNG TIN ĐỂ THANH TOÁN LẠI
            bookingTransportMapper.updateBookingTransportFromRequest(request, booking);

            if (request.getQuantityTickets() != null && request.getQuantityTickets() > 0) {
                BigDecimal ticketPrice = booking.getTransportTicket().getPrice() != null ? booking.getTransportTicket().getPrice() : BigDecimal.ZERO;
                booking.setPriceBookingTransport(ticketPrice.multiply(BigDecimal.valueOf(request.getQuantityTickets())));
            }

            bookingTransportRepository.save(booking);

            Payment payment = paymentRepository.findByBookingTransport_IdBookingTransport(idBookingTransport);
            if (payment != null) {
                payment.setStatus(PredefinedStatusPayment.STATUS_PAYMENT_FAILED);
                paymentRepository.save(payment);
            }

            // Chỉ gửi thông báo cho Chủ vé (Business): User đã thay đổi thông tin
            NotificationCreatedEvent ownerEvent = NotificationCreatedEvent.builder()
                    .idNotification(UUID.randomUUID())
                    .idUser(booking.getTransportTicket().getUsers().getIdUser())
                    .title("Khách hàng cập nhật đơn đặt vé")
                    .content(String.format("Khách hàng %s đã cập nhật thông tin hoặc đang thực hiện thanh toán lại cho đơn đặt vé của bạn.", booking.getUsers().getLastName()))
                    .type("booking")
                    .idRef(booking.getIdBookingTransport().toString())
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            try {
                rabbitTemplate.convertAndSend(rabbitConfig.EXCHANGE_NOTIFICATION, rabbitConfig.ROUTING_KEY_NOTIFICATION, objectMapper.writeValueAsString(ownerEvent));
            } catch (Exception ex) {
                log.error("Lỗi khi gửi thông báo user cập nhật vé cho Business: {}", ex.getMessage());
            }

            return self.callPaymentApi(booking, PredefinedMethodPayment.ZALOPAY_METHOD, null);
        }
    }

    @Transactional
    public String cancelBookingTransport(UUID idBookingTransport) {
        if (paymentRepository.existsByBookingTransport_IdBookingTransportAndStatus(idBookingTransport, PredefinedStatusPayment.STATUS_PAYMENT_SUCCESS)) {
            throw new ApiException(ErrorCode.PAID);
        }

        BookingTransport booking = bookingTransportRepository.findById(idBookingTransport).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        Payment payment = paymentRepository.findByBookingTransport_IdBookingTransport(idBookingTransport);
        if (payment != null) {
            payment.setStatus(PredefinedStatusPayment.STATUS_PAYMENT_FAILED);
            paymentRepository.save(payment);
        }

        booking.setStatus(PredefinedStatusBookingTransport.STATUS_BOOKING_FAILED);
        bookingTransportRepository.save(booking);

        return "Hủy đặt vé di chuyển thành công!";
    }
}