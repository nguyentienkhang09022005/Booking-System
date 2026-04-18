package com.example.java_service.Services;

import com.example.java_service.Configs.RabbitConfig;
import com.example.java_service.Constant.*;
import com.example.java_service.Dto.Request.*;
import com.example.java_service.Dto.Response.*;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Helpers.VoucherValidationHelper;
import com.example.java_service.Mappers.BookingMapper;
import com.example.java_service.Mappers.RoomMapper;
import com.example.java_service.Mappers.ServiceMapper;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingHandleService
{
    BookingRepository bookingRepository;
    BookingRoomRepository bookingRoomRepository;
    BookingServiceRepository bookingServiceRepository;
    PaymentRepository paymentRepository;
    RoomRepository roomRepository;
    ServiceRepository serviceRepository;
    UserRepository userRepository;
    AccommodationRepository accommodationRepository;
    VoucherRepository voucherRepository;
    UserVoucherRepository userVoucherRepository;
    AppliedVoucherRepository appliedVoucherRepository;
    ImagesRepository imagesRepository;
    BookingMapper bookingMapper;
    RoomMapper roomMapper;
    ServiceMapper serviceMapper;
    VoucherMapper voucherMapper;
    ObjectMapper objectMapper;
    BookingRoomService bookingRoomService;
    BookingServiceService bookingServiceService;
    UserVoucherService userVoucherService;
    AppliedVoucherService appliedVoucherService;
    RabbitTemplate rabbitTemplate;
    RabbitConfig rabbitConfig;
    VoucherValidationHelper voucherValidationHelper;

    @NonFinal //
    BookingHandleService self;

    WebClient webClient;

    @Autowired
    @Lazy
    public void setSelf(BookingHandleService self) {
        this.self = self;
    }

    // Hàm lấy danh sách đặt phòng của khách sạn (cho company)
    public List<BookingListResponse> getListBooking(UUID idAccommodation)
    {
        accommodationRepository.findById(idAccommodation).orElseThrow(
                () -> new ApiException(ErrorCode.ACCOMMODATION_NOT_FOUND)
        );

        // Lấy tất cả danh sách booking
        List<Booking> bookingList = bookingRepository.findByAccommodation_IdAccommodation(idAccommodation);
        if (bookingList.isEmpty()) {
            return List.of();
        }

        return bookingList.stream()
                .map(booking -> {
                    BookingListResponse response = bookingMapper.toBookingListResponse(booking);
                    return response;
                })
                .toList();
    }

    // Hàm lấy lịch sử đặt phòng của người dùng (cho user)
    public List<BookingListResponse> getHistoryListBooking(UUID idUser)
    {
        userRepository.findById(idUser).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        // Lấy tất cả danh sách booking
        List<Booking> bookingList = bookingRepository.findByUsers_IdUser(idUser);
        if (bookingList.isEmpty()) {
            return List.of();
        }

        return bookingList.stream()
                .map(booking -> {
                    BookingListResponse response = bookingMapper.toBookingListResponse(booking);
                    return response;
                })
                .toList();
    }

    // Hàm lấy chi tiết thông tin đặt phòng
    public BookingResponse getInfBooking(UUID idBooking)
    {
        Booking booking = bookingRepository.findById(idBooking)
                .orElseThrow(() -> new ApiException(ErrorCode.BOOKING_NOT_FOUND));

        // Lấy toàn bộ dữ liệu liên quan
        List<BookingRoom> bookingRooms = bookingRoomRepository.findAllBookingRoomByBookingIds(List.of(idBooking));
        List<BookingService> bookingServices = bookingServiceRepository.findAllBookingServiceByBookingIds(List.of(idBooking));
        List<Payment> payments = paymentRepository.findByBooking_IdBookingIn(List.of(idBooking));

        // Gom nhóm dữ liệu
        Map<UUID, List<BookingRoom>> bookingRoomMap = bookingRooms.stream()
                .collect(Collectors.groupingBy(br -> br.getBooking().getIdBooking()));

        Map<UUID, List<BookingService>> bookingServiceMap = bookingServices.stream()
                .collect(Collectors.groupingBy(bs -> bs.getBooking().getIdBooking()));

        Map<UUID, Payment> paymentMap = payments.stream()
                .collect(Collectors.toMap(p -> p.getBooking().getIdBooking(), p -> p));

        BookingResponse response = bookingMapper.toBookingResponse(booking);

        // Payment Response
        Payment payment = paymentMap.get(booking.getIdBooking());
        if (payment != null) {
            response.setPaymentInBookingResponse(bookingMapper.toPaymentInBookingResponse(payment));
        }

        // Booking Room Response
        List<BookingRoom> bookingRoomsOfBooking = bookingRoomMap.getOrDefault(booking.getIdBooking(), List.of());
        List<BookingRoomInBookingResponse> bookingRoomResponses = bookingRoomsOfBooking.stream()
                .map(bookingMapper::toBookingRoomInBookingResponse)
                .toList();
        response.setBookingRoomInBookingResponses(bookingRoomResponses);

        // Booking Service Response
        List<BookingService> bookingServicesOfBooking = bookingServiceMap.getOrDefault(booking.getIdBooking(), List.of());
        List<BookingServiceInBookingResponse> bookingServiceResponses = bookingServicesOfBooking.stream()
                .map(bookingMapper::toBookingServiceInBookingResponse)
                .toList();
        response.setBookingServiceInBookingResponses(bookingServiceResponses);

        // Room Response
        List<BookingRoom> roomsOfBooking = bookingRoomMap.getOrDefault(booking.getIdBooking(), List.of());
        List<BookingRoomInBookingResponse> roomResponses = roomsOfBooking.stream()
                .map(br -> {
                    BookingRoomInBookingResponse responses = bookingMapper.toBookingRoomInBookingResponse(br);
                    if (br.getRoom() != null) {
                        RoomResponse roomResponse = roomMapper.toRoomResponse(br.getRoom());
                        responses.setRoomResponses(List.of(roomResponse));
                    } else {
                        responses.setRoomResponses(List.of());
                    }
                    return responses;
                })
                .toList();
        response.setBookingRoomInBookingResponses(roomResponses);

        // Service Response
        List<BookingService> servicesOfBooking = bookingServiceMap.getOrDefault(booking.getIdBooking(), List.of());
        List<BookingServiceInBookingResponse> serviceResponses = servicesOfBooking.stream()
                .map(bs -> {
                    BookingServiceInBookingResponse responses = bookingMapper.toBookingServiceInBookingResponse(bs);
                    if (bs.getService() != null) {
                        ServiceResponse serviceResponse = serviceMapper.toServiceResponse(bs.getService());
                        responses.setServiceResponses(List.of(serviceResponse));
                    } else {
                        responses.setServiceResponses(List.of());
                    }
                    return responses;
                })
                .toList();
        response.setBookingServiceInBookingResponses(serviceResponses);

        // Voucher Used Response
        List<AppliedVoucher> appliedVouchers = appliedVoucherRepository.findAppliedVouchersWithVoucherByBookingId(booking.getIdBooking());

        // Lấy ID của tất cả voucher
        List<String> voucherIds = appliedVouchers.stream()
                .map(appliedVoucher -> appliedVoucher.getVoucher().getIdVoucher().toString())
                .toList();

        // Lấy tất cả ảnh
        List<Images> images = imagesRepository.findByRefIdIn(voucherIds);
        Map<String, String> voucherImagesMap = images.stream()
                .collect(Collectors.toMap(
                        Images::getRefId,
                        Images::getUrl
                ));

        List<VoucherViewResponse> voucherViewResponses = appliedVouchers.stream()
                .map(av -> {
                    VoucherViewResponse voucherViewResponse = voucherMapper.toVoucherViewResponse(av.getVoucher());
                    String idVoucherStr = av.getVoucher().getIdVoucher().toString();
                    voucherViewResponse.setUrlImage(voucherImagesMap.get(idVoucherStr));
                    return voucherViewResponse;
                })
                .toList();
        response.setVoucherViewResponses(voucherViewResponses);

        return response;
    }

    // Hàm tạo đặt phòng
    public PaymentResponse saveBooking(BookingRequest bookingRequest)
    {
        Booking booking = self.createBookingTransactional(bookingRequest);

        return self.callPaymentApi(booking, bookingRequest);
    }

    @Transactional(rollbackFor = Exception.class)
    public Booking createBookingTransactional(BookingRequest bookingRequest)
    {
        try {
            Users user = userRepository.findById(bookingRequest.getIdUser()).orElseThrow(
                    () -> new ApiException(ErrorCode.USER_NOT_FOUND)
            );

            Accommodation accommodation = accommodationRepository.findByIdWithUsers(bookingRequest.getIdAccommodation());
            if (accommodation == null) {
                throw new ApiException(ErrorCode.ACCOMMODATION_NOT_FOUND);
            }

            if (!bookingRequest.getCheckoutDate().isAfter(bookingRequest.getCheckinDate())) {
                throw new ApiException(ErrorCode.CHECKOUT_DATE_BEFORE_CHECKIN_DATE);
            }

            Booking booking = bookingMapper.toBooking(bookingRequest);
            booking.setStatus(PredefinedStatusBooking.STATUS_BOOKING_PENDING);
            booking.setUsers(user);
            booking.setAccommodation(accommodation);
            Booking savedBooking = bookingRepository.save(booking);

            BookingRoomResponse bookingRoomResponse = bookingRoomService.saveBookingRoom(bookingRequest.getBookingRooms(),
                    savedBooking.getIdBooking());
            BookingServiceResponse bookingServiceResponse = bookingServiceService.saveBookingService(bookingRequest.getBookingServices(),
                    savedBooking.getIdBooking());

            // Lưu tổng tiền của lần booking khi chưa có voucher
            BigDecimal totalPriceRoom = BigDecimal.ZERO;
            BigDecimal totalPriceService = BigDecimal.ZERO;
            if (bookingRoomResponse != null && bookingRoomResponse.getPriceBookingRoom() != null) {
                totalPriceRoom = bookingRoomResponse.getPriceBookingRoom();
            }

            if (bookingServiceResponse != null && bookingServiceResponse.getPriceBookingService() != null) {
                totalPriceService = bookingServiceResponse.getPriceBookingService();
            }

            // Tính tổng tiền đặt phòng theo thời gian đặt
            BigDecimal finalRoomPrice = BigDecimal.ZERO;
            long daysBetween = ChronoUnit.DAYS.between(
                    bookingRequest.getCheckinDate().toLocalDate(),
                    bookingRequest.getCheckoutDate().toLocalDate()
            );

            if (daysBetween >= 1) {
                finalRoomPrice = totalPriceRoom.multiply(BigDecimal.valueOf(daysBetween));
            } else {
                long minutesUsed = ChronoUnit.MINUTES.between(
                        bookingRequest.getCheckinDate(),
                        bookingRequest.getCheckoutDate());
                if (minutesUsed < 0) {
                    minutesUsed = 0;
                }
                BigDecimal percentage = BigDecimal.valueOf(minutesUsed).divide(BigDecimal.valueOf(1440), 4, RoundingMode.HALF_UP);
                finalRoomPrice = totalPriceRoom.multiply(percentage);
            }

            // Gán giá trị để áp dụng Voucher
            BigDecimal remainingRoomPrice = finalRoomPrice;
            BigDecimal remainingServicePrice = totalPriceService;
            BigDecimal totalPrice = remainingRoomPrice.add(remainingServicePrice);

            // Xữ lý nếu có voucher
            if (bookingRequest.getUserVoucherRequests() != null && !bookingRequest.getUserVoucherRequests().isEmpty()) {
                List<UUID> voucherIds = bookingRequest.getUserVoucherRequests().stream()
                        .map(UserVoucherRequest::getIdVoucher)
                        .toList();
                List<Voucher> vouchers = voucherRepository.findAllWithAccommodationAndUsersByIds(voucherIds);
                Map<UUID, Voucher> voucherMap = vouchers.stream().collect(Collectors.toMap(Voucher::getIdVoucher, v -> v));
                voucherValidationHelper.validateVouchersForAccommodation(vouchers, accommodation.getIdAccommodation(), accommodation.getUsers().getIdUser());

                for (UserVoucherRequest userVoucherRequest : bookingRequest.getUserVoucherRequests())
                {
                    Voucher voucher = voucherMap.get(userVoucherRequest.getIdVoucher());
                    if (voucher == null) {
                        throw new ApiException(ErrorCode.VOUCHER_NOT_FOUND);
                    }

                    // Lưu voucher
                    userVoucherService.saveUserVoucherForBooking(userVoucherRequest);

                    // Lưu voucher đã sử dụng
                    AppliedVoucherRequest appliedVoucherRequest = AppliedVoucherRequest.builder()
                            .idUser(userVoucherRequest.getIdUser())
                            .idVoucher(userVoucherRequest.getIdVoucher())
                            .build();
                    appliedVoucherService.saveAppliedVoucher(appliedVoucherRequest, savedBooking.getIdBooking());

                    // Áp dụng voucher theo hạng mục
                    if (voucher.getDiscountPercent() != null && voucher.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0)
                    {
                        BigDecimal targetPriceForDiscount = BigDecimal.ZERO;
                        String applyTo = voucher.getApplyTo() != null ? voucher.getApplyTo().toUpperCase() : PredefinedApplyTo.APPLY_TO_ALL;

                        switch (applyTo) {
                            case PredefinedApplyTo.APPLY_TO_ROOM:
                                targetPriceForDiscount = remainingRoomPrice;
                                break;
                            case PredefinedApplyTo.APPLY_TO_SERVICE:
                                targetPriceForDiscount = remainingServicePrice;
                                break;
                            case PredefinedApplyTo.APPLY_TO_ALL:
                            default:
                                targetPriceForDiscount = totalPrice;
                                break;
                        }

                        // Nếu mục tiêu giảm giá bằng 0 (VD: dùng voucher Service nhưng không mua service nào)
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

                        // Không giảm vượt quá số tiền của hạng mục đó
                        if (discount.compareTo(targetPriceForDiscount) > 0) {
                            discount = targetPriceForDiscount;
                        }

                        // Trừ tiền vào biến còn lại để áp dụng chuẩn nếu có nhiều voucher
                        switch (applyTo) {
                            case PredefinedApplyTo.APPLY_TO_ROOM:
                                remainingRoomPrice = remainingRoomPrice.subtract(discount);
                                break;
                            case PredefinedApplyTo.APPLY_TO_SERVICE:
                                remainingServicePrice = remainingServicePrice.subtract(discount);
                                break;
                            case PredefinedApplyTo.APPLY_TO_ALL:
                            default:
                                // Nếu giảm ALL, ưu tiên trừ vào tiền phòng trước
                                if (discount.compareTo(remainingRoomPrice) <= 0) {
                                    remainingRoomPrice = remainingRoomPrice.subtract(discount);
                                } else {
                                    // Nếu tiền phòng không đủ trừ, số âm còn dư sẽ đẩy sang trừ vào tiền dịch vụ
                                    BigDecimal leftoverDiscount = discount.subtract(remainingRoomPrice);
                                    remainingRoomPrice = BigDecimal.ZERO;
                                    remainingServicePrice = remainingServicePrice.subtract(leftoverDiscount);
                                }
                                break;
                        }

                        totalPrice = remainingRoomPrice.add(remainingServicePrice);
                    }
                }
            }

            booking.setTotalPrice(totalPrice);
            bookingRepository.save(booking);

            // Gửi sự kiện vào Exchanges
            NotificationCreatedEvent event = NotificationCreatedEvent.builder()
                    .idNotification(UUID.randomUUID())
                    .idUser(booking.getUsers().getIdUser())
                    .title("Thông báo đặt phòng")
                    .content(String.format("Thông tin đơn đặt phòng nơi cư trú %s của bạn đã được ghi nhận!", booking.getAccommodation().getAccommodationName()))
                    .type("booking")
                    .idRef(booking.getIdBooking().toString())
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            try {
                String message = objectMapper.writeValueAsString(event);
                rabbitTemplate.convertAndSend(
                        rabbitConfig.EXCHANGE_NOTIFICATION,
                        rabbitConfig.ROUTING_KEY_NOTIFICATION,
                        message
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return booking;
        } catch (Exception e)
        {
            log.error("Lỗi đặt phòng: {}", e.getMessage());
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentResponse callPaymentApi(Booking booking, BookingRequest bookingRequest)
    {
        try
        {
            // Branch on payment method
            if (PredefinedMethodPayment.VND_STABLE_COIN.equals(bookingRequest.getPaymentMethod()) && bookingRequest.getPayerAddress() != null && !bookingRequest.getPayerAddress().isBlank())
            {
                // Call Go service VND payment prepare endpoint
                Map<String, Object> vndRequest = Map.of(
                        "id_user", booking.getUsers().getIdUser(),
                        "id_booking", booking.getIdBooking(),
                        "amount", booking.getTotalPrice().doubleValue(),
                        "payer_address", bookingRequest.getPayerAddress()
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
                        .amount(booking.getTotalPrice())
                        .method(PredefinedMethodPayment.VND_STABLE_COIN)
                        .status("Pending")
                        .typedData((Map<String, Object>) vndResponse.get("typed_data"))
                        .orderId(vndResponse.get("order_id") != null ? vndResponse.get("order_id").toString() : null)
                        .vndTokenAddress(vndResponse.get("vnd_token_address") != null ? vndResponse.get("vnd_token_address").toString() : null)
                        .build();
            }
            else
            {
                // Default: ZaloPay flow (existing behaviour)
                PaymentRequest paymentRequest = new PaymentRequest(
                        booking.getUsers().getIdUser(),
                        booking.getIdBooking(),
                        null,
                        null,
                        booking.getTotalPrice().doubleValue()
                );

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
        }
        catch (ApiException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Lỗi khi gọi API thanh toán: {}", e.getMessage(), e);
            booking.setStatus(PredefinedStatusBooking.STATUS_BOOKING_FAILED);
            bookingRepository.save(booking);
            throw new ApiException(ErrorCode.BOOKING_FAILED);
        }
    }

    @Transactional
    // Hàm cập nhật lại thông tin đặt phòng khi chưa thanh toán cho (user)
    public PaymentResponse updateBooking(BookingUpdateRequest bookingUpdateRequest, UUID idBooking)
    {
        // Kiểm tra đã thanh toán chưa
        if (paymentRepository.existsByBooking_IdBookingAndStatus(idBooking, PredefinedStatusPayment.STATUS_PAYMENT_SUCCESS)){
            throw new ApiException(ErrorCode.PAID);
        }
        Booking booking = bookingRepository.findById(idBooking).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        if (!bookingUpdateRequest.getCheckoutDate().isAfter(bookingUpdateRequest.getCheckinDate())) {
            throw new ApiException(ErrorCode.CHECKOUT_DATE_BEFORE_CHECKIN_DATE);
        }

        bookingMapper.updateBookingFromRequest(bookingUpdateRequest, booking);

        // Nếu có thay đổi thông tin đặt phòng
        BookingRoomResponse bookingRoomResponse = null;
        if (bookingUpdateRequest.getBookingRooms() != null && !bookingUpdateRequest.getBookingRooms().isEmpty())
        {
            List<BookingRoom> oldListBookingRoom = bookingRoomRepository.findByBooking_IdBooking(idBooking);

            if (!oldListBookingRoom.isEmpty())
            {
                // Gom nhóm số lượng room đặt theo từng booking room
                Map<UUID, Integer> restoreQuantityRooms = oldListBookingRoom.stream()
                        .collect(Collectors.groupingBy(
                                br -> br.getRoom().getIdRoom(),
                                Collectors.summingInt(BookingRoom::getQuantity)
                        ));

                // Hoàn lại số lượng cho room và cập nhật status nếu số lượng lớn hơn 0
                restoreQuantityRooms.forEach((idRoom, quantity) -> {
                    roomRepository.restoreRoomQuantity(idRoom, quantity);
                    roomRepository.updateStatusIfAvailable(idRoom, PredefinedStatusRoom.STATUS_ROOM_AVAILABLE);
                });

                bookingRoomRepository.deleteByBooking_IdBooking(idBooking);
            }
            bookingRoomResponse = bookingRoomService.saveBookingRoom(bookingUpdateRequest.getBookingRooms(), idBooking);
        }

        // Nếu có thay đổi thông tin đặt dịch vụ
        BookingServiceResponse bookingServiceResponse = null;
        if (bookingUpdateRequest.getBookingServices() != null && !bookingUpdateRequest.getBookingServices().isEmpty())
        {
            List<BookingService> oldListBookingService = bookingServiceRepository.findByBooking_IdBooking(idBooking);

            if (!oldListBookingService.isEmpty())
            {
                // Gom nhóm số lượng service đặt theo từng booking service
                Map<UUID, Integer> restoreQuantityServices = oldListBookingService.stream()
                        .collect(Collectors.groupingBy(
                                bs -> bs.getService().getIdService(),
                                Collectors.summingInt(BookingService::getQuantity)
                        ));

                // Hoàn lại số lượng cho service và cập nhật status nếu số lượng lớn hơn 0
                restoreQuantityServices.forEach((idService, quantity) -> {
                    serviceRepository.restoreServiceQuantity(idService, quantity);
                    serviceRepository.updateStatusIfAvailable(idService, PredefinedStatusService.STATUS_SERVICE_AVAILABLE);
                });

                bookingServiceRepository.deleteByBooking_IdBooking(idBooking);
            }
            bookingServiceResponse = bookingServiceService.saveBookingService(bookingUpdateRequest.getBookingServices(),
                    idBooking);
        }

        // Lưu tổng tiền của lần booking
        BigDecimal totalPriceRoom = BigDecimal.ZERO;
        BigDecimal totalPriceService = BigDecimal.ZERO;
        if (bookingRoomResponse != null && bookingRoomResponse.getPriceBookingRoom() != null) {
            totalPriceRoom = bookingRoomResponse.getPriceBookingRoom();
        }

        if (bookingServiceResponse != null && bookingServiceResponse.getPriceBookingService() != null) {
            totalPriceService = bookingServiceResponse.getPriceBookingService();
        }

        // Tính tổng tiền đặt phòng theo thời gian đặt
        BigDecimal finalRoomPrice = BigDecimal.ZERO;
        long daysBetween = ChronoUnit.DAYS.between(
                bookingUpdateRequest.getCheckinDate().toLocalDate(),
                bookingUpdateRequest.getCheckoutDate().toLocalDate()
        );

        if (daysBetween >= 1) {
            finalRoomPrice = totalPriceRoom.multiply(BigDecimal.valueOf(daysBetween));
        } else {
            long minutesUsed = ChronoUnit.MINUTES.between(
                    bookingUpdateRequest.getCheckinDate(),
                    bookingUpdateRequest.getCheckoutDate());
            if (minutesUsed < 0) {
                minutesUsed = 0;
            }
            BigDecimal percentage = BigDecimal.valueOf(minutesUsed).divide(BigDecimal.valueOf(1440), 4, RoundingMode.HALF_UP);
            finalRoomPrice = totalPriceRoom.multiply(percentage);
        }

        // Gán giá trị để áp dụng Voucher
        BigDecimal remainingRoomPrice = finalRoomPrice;
        BigDecimal remainingServicePrice = totalPriceService;
        BigDecimal totalPrice = remainingRoomPrice.add(remainingServicePrice);

            // Nếu có cập nhật voucher
            if (bookingUpdateRequest.getUserVoucherRequests() != null && !bookingUpdateRequest.getUserVoucherRequests().isEmpty()) {
                //----------Xữ lý voucher đã tạo trước đó---------------
                // Xóa danh sách voucher đã apply
                appliedVoucherService.deleteAppliedVoucher(booking.getIdBooking());

                // Reset lại is_used và usedAt của voucher còn hạn
                List<UserVoucher> usedVouchers = userVoucherRepository.findUsedVouchersByBookingId(booking.getIdBooking());
                usedVouchers.forEach(uv -> {
                    uv.setIsUsed(false);
                    uv.setUsedAt(null);
                });
                userVoucherRepository.saveAll(usedVouchers);

                List<UUID> voucherIds = bookingUpdateRequest.getUserVoucherRequests().stream()
                        .map(UserVoucherRequest::getIdVoucher)
                        .toList();
                List<Voucher> vouchers = voucherRepository.findAllWithAccommodationAndUsersByIds(voucherIds);
                Map<UUID, Voucher> voucherMap = vouchers.stream().collect(Collectors.toMap(Voucher::getIdVoucher, v -> v));
                voucherValidationHelper.validateVouchersForAccommodation(vouchers, booking.getAccommodation().getIdAccommodation(), booking.getAccommodation().getUsers().getIdUser());

                for (UserVoucherRequest userVoucherRequest : bookingUpdateRequest.getUserVoucherRequests())
                {
                    Voucher voucher = voucherMap.get(userVoucherRequest.getIdVoucher());
                    if (voucher == null) {
                        throw new ApiException(ErrorCode.VOUCHER_NOT_FOUND);
                    }

                // Lưu voucher
                userVoucherService.saveUserVoucherForBooking(userVoucherRequest);

                // Lưu voucher đã sử dụng
                AppliedVoucherRequest appliedVoucherRequest = AppliedVoucherRequest.builder()
                        .idUser(userVoucherRequest.getIdUser())
                        .idVoucher(userVoucherRequest.getIdVoucher())
                        .build();
                appliedVoucherService.saveAppliedVoucher(appliedVoucherRequest, booking.getIdBooking());

                // Áp dụng voucher theo hạng mục
                if (voucher.getDiscountPercent() != null && voucher.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0)
                {
                    BigDecimal targetPriceForDiscount = BigDecimal.ZERO;
                    String applyTo = voucher.getApplyTo() != null ? voucher.getApplyTo().toUpperCase() : PredefinedApplyTo.APPLY_TO_ALL;

                    switch (applyTo) {
                        case PredefinedApplyTo.APPLY_TO_ROOM:
                            targetPriceForDiscount = remainingRoomPrice;
                            break;
                        case PredefinedApplyTo.APPLY_TO_SERVICE:
                            targetPriceForDiscount = remainingServicePrice;
                            break;
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

                    // Không giảm vượt quá số tiền của hạng mục đó
                    if (discount.compareTo(targetPriceForDiscount) > 0) {
                        discount = targetPriceForDiscount;
                    }

                    // Trừ tiền vào biến còn lại để áp dụng chuẩn nếu có nhiều voucher
                    switch (applyTo) {
                        case PredefinedApplyTo.APPLY_TO_ROOM:
                            remainingRoomPrice = remainingRoomPrice.subtract(discount);
                            break;
                        case PredefinedApplyTo.APPLY_TO_SERVICE:
                            remainingServicePrice = remainingServicePrice.subtract(discount);
                            break;
                        case PredefinedApplyTo.APPLY_TO_ALL:
                        default:
                            // Nếu giảm ALL, ưu tiên trừ vào tiền phòng trước
                            if (discount.compareTo(remainingRoomPrice) <= 0) {
                                remainingRoomPrice = remainingRoomPrice.subtract(discount);
                            } else {
                                // Nếu tiền phòng không đủ trừ, số âm còn dư sẽ đẩy sang trừ vào tiền dịch vụ
                                BigDecimal leftoverDiscount = discount.subtract(remainingRoomPrice);
                                remainingRoomPrice = BigDecimal.ZERO;
                                remainingServicePrice = remainingServicePrice.subtract(leftoverDiscount);
                            }
                            break;
                    }

                    totalPrice = remainingRoomPrice.add(remainingServicePrice);
                }
            }
        }

        booking.setTotalPrice(totalPrice);
        bookingRepository.save(booking);

        paymentRepository.deleteByBooking_IdBooking(idBooking); // Xóa payment cũ

        BookingRequest tempPaymentRequest = BookingRequest.builder()
                .currency(bookingUpdateRequest.getCurrency() != null ? bookingUpdateRequest.getCurrency() : "VND")
                .paymentMethod(bookingUpdateRequest.getPaymentMethod())
                .payerAddress(bookingUpdateRequest.getPayerAddress())
                .build();

        return self.callPaymentApi(booking, tempPaymentRequest);
    }

    //Hàm checkout đặt phòng cho (company)
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<String> checkoutBooking(BookingCheckoutRequest request, UUID idBooking)
    {
        // Kiểm tra người dùng đã thanh toán chưa
        if (paymentRepository.existsByBooking_IdBookingAndStatus(idBooking, PredefinedStatusPayment.STATUS_PAYMENT_DEFAULT)){
            throw new ApiException(ErrorCode.USER_UNPAID);
        }

        Booking booking = bookingRepository.findById(idBooking).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );
        List<BookingRoom> bookingRooms = bookingRoomRepository.findAllBookingRoomByBookingIds(List.of(idBooking));
        List<BookingService> bookingServices = bookingServiceRepository.findByBooking_IdBooking(idBooking);

        BigDecimal totalPriceBookingRoom = bookingRooms
                .stream()
                .map(BookingRoom::getPriceBookingRoom)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPriceBooking = booking.getTotalPrice();
        if (booking.getCheckinDate().isAfter(request.getCheckinDateActual()))
        {
            long minutesDiff  = ChronoUnit.MINUTES.between(
                    request.getCheckinDateActual(),
                    booking.getCheckinDate());
            if (minutesDiff  < 0) {
                minutesDiff  = 0;
            }

            // Tính phần trăm số tiền nếu checkin sớm
            long daysEarly = minutesDiff / 1440;
            long minutesEarly = minutesDiff % 1440;

            BigDecimal priceEarly = totalPriceBookingRoom
                    .multiply(BigDecimal.valueOf(daysEarly))
                    .add(totalPriceBookingRoom.multiply(BigDecimal.valueOf(minutesEarly).divide(BigDecimal.valueOf(1440), 4, RoundingMode.HALF_UP)));

            totalPriceBooking = totalPriceBooking.add(priceEarly);
        }

        if (booking.getCheckoutDate().isBefore(request.getCheckoutDateActual()))
        {
            long minutesDiff = ChronoUnit.MINUTES.between(
                    booking.getCheckoutDate(),
                    request.getCheckoutDateActual());
            if (minutesDiff < 0) {
                minutesDiff = 0;
            }

            // Tính phần trăm số tiền nếu checkout muộn
            long daysLate = minutesDiff / 1440;
            long minutesLate = minutesDiff % 1440;

            BigDecimal priceLate = totalPriceBookingRoom
                    .multiply(BigDecimal.valueOf(daysLate))
                    .add(totalPriceBookingRoom.multiply(BigDecimal.valueOf(minutesLate).divide(BigDecimal.valueOf(1440), 4, RoundingMode.HALF_UP)));

            totalPriceBooking = totalPriceBooking.add(priceLate);
        }

        // Hoàn lại số lượng room
        if (!bookingRooms.isEmpty())
        {
            // Gom nhóm số lượng room đặt theo từng booking room
            Map<UUID, Integer> restoreQuantityRooms = bookingRooms.stream()
                    .collect(Collectors.groupingBy(
                            br -> br.getRoom().getIdRoom(),
                            Collectors.summingInt(BookingRoom::getQuantity)
                    ));

            // Hoàn lại số lượng cho room và cập nhật status nếu số lượng lớn hơn 0
            restoreQuantityRooms.forEach((idRoom, quantity) -> {
                roomRepository.restoreRoomQuantity(idRoom, quantity);
                roomRepository.updateStatusIfAvailable(idRoom, PredefinedStatusRoom.STATUS_ROOM_AVAILABLE);
            });
        }

        // Hoàn lại số lượng Service
        if (!bookingServices.isEmpty())
        {
            // Gom nhóm số lượng service đặt theo từng booking service
            Map<UUID, Integer> restoreQuantityServices = bookingServices.stream()
                    .collect(Collectors.groupingBy(
                            bs -> bs.getService().getIdService(),
                            Collectors.summingInt(BookingService::getQuantity)
                    ));

            // Hoàn lại số lượng cho service và cập nhật status nếu số lượng lớn hơn 0
            restoreQuantityServices.forEach((idService, quantity) -> {
                serviceRepository.restoreServiceQuantity(idService, quantity);
                serviceRepository.updateStatusIfAvailable(idService, PredefinedStatusService.STATUS_SERVICE_AVAILABLE);
            });
        }

        booking.setTotalPrice(totalPriceBooking);
        booking.setStatus(PredefinedStatusBooking.STATUS_BOOKING_CHECKED_OUT);
        bookingRepository.save(booking);

        // Gửi sự kiện vào Exchanges
        NotificationCreatedEvent event = NotificationCreatedEvent.builder()
                .idNotification(UUID.randomUUID())
                .idUser(booking.getUsers().getIdUser())
                .title("Thông báo đặt phòng")
                .content(String.format("Bạn đã trả phòng nơi cư trú %s thành công!", booking.getAccommodation().getAccommodationName()))
                .type("booking")
                .idRef(booking.getIdBooking().toString())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            String message = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(
                    rabbitConfig.EXCHANGE_NOTIFICATION,
                    rabbitConfig.ROUTING_KEY_NOTIFICATION,
                    message
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return ResponseEntity.ok("Cập nhật trả phòng thành công!");
    }

    // Hàm cập nhật status đặt phòng cho (company)
    public ResponseEntity<String> updateStatusBooking(BookingUpdateStatusRequest request, UUID idBooking)
    {
        // Kiểm tra người dùng đã thanh toán chưa
        if (paymentRepository.existsByBooking_IdBookingAndStatus(idBooking, PredefinedStatusPayment.STATUS_PAYMENT_DEFAULT)){
            throw new ApiException(ErrorCode.USER_UNPAID);
        }

        Booking booking = bookingRepository.findById(idBooking).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        booking.setStatus(request.getStatus());
        bookingRepository.save(booking);

        // Gửi sự kiện vào Exchanges
        NotificationCreatedEvent event = NotificationCreatedEvent.builder()
                .idNotification(UUID.randomUUID())
                .idUser(booking.getUsers().getIdUser())
                .title("Thông báo trạng thái đặt phòng")
                .content(String.format("Trạng thái đơn đặt phòng nơi cư trú %s của bạn đã thay đổi!", booking.getAccommodation().getAccommodationName()))
                .type("booking")
                .idRef(booking.getIdBooking().toString())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            String message = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(
                    rabbitConfig.EXCHANGE_NOTIFICATION,
                    rabbitConfig.ROUTING_KEY_NOTIFICATION,
                    message
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return ResponseEntity.ok("Cập nhật trạng thái thành công!");
    }

    // Hàm xóa đặt phòng
    @Transactional
    public ResponseEntity<String> deleteBookingRoom(UUID idBooking)
    {
        Booking booking = bookingRepository.findById(idBooking)
                .orElseThrow(() -> new ApiException(ErrorCode.BOOKING_NOT_FOUND));

        // Kiểm tra nếu Booking đã bị user hủy rồi thì không hoàn số lượng service và room lại nữa
        if (PredefinedStatusBooking.STATUS_BOOKING_FAILED.equals(booking.getStatus())) {
            bookingRepository.delete(booking);
            return ResponseEntity.ok("Xóa đặt phòng thành công!");
        }

        // Lấy danh sách booking room và booking service tương ứng
        List<BookingRoom> listBookingRoom = bookingRoomRepository.findByBooking_IdBooking(idBooking);
        List<BookingService> listBookingService = bookingServiceRepository.findByBooking_IdBooking(idBooking);

        if (!listBookingRoom.isEmpty())
        {
            // Gom nhóm số lượng room đặt theo từng booking room
            Map<UUID, Integer> restoreQuantityRooms = listBookingRoom.stream()
                    .collect(Collectors.groupingBy(
                            br -> br.getRoom().getIdRoom(),
                            Collectors.summingInt(BookingRoom::getQuantity)
                    ));

            // Hoàn lại số lượng cho room và cập nhật status nếu số lượng lớn hơn 0
            restoreQuantityRooms.forEach((idRoom, quantity) -> {
                roomRepository.restoreRoomQuantity(idRoom, quantity);
                roomRepository.updateStatusIfAvailable(idRoom, PredefinedStatusRoom.STATUS_ROOM_AVAILABLE);
            });
        }

        if (!listBookingService.isEmpty())
        {
            // Gom nhóm số lượng service đặt theo từng booking service
            Map<UUID, Integer> restoreQuantityServices = listBookingService.stream()
                    .collect(Collectors.groupingBy(
                            bs -> bs.getService().getIdService(),
                            Collectors.summingInt(BookingService::getQuantity)
                    ));

            // Hoàn lại số lượng cho service và cập nhật status nếu số lượng lớn hơn 0
            restoreQuantityServices.forEach((idService, quantity) -> {
                serviceRepository.restoreServiceQuantity(idService, quantity);
                serviceRepository.updateStatusIfAvailable(idService, PredefinedStatusService.STATUS_SERVICE_AVAILABLE);
            });
        }

        //----------Xữ lý voucher đã tạo trước đó---------------
        // Reset lại is_used và usedAt của voucher còn hạn
        List<UserVoucher> usedVouchers = userVoucherRepository.findUsedVouchersByBookingId(booking.getIdBooking());
        usedVouchers.forEach(uv -> {
            uv.setIsUsed(false);
            uv.setUsedAt(null);
        });
        userVoucherRepository.saveAll(usedVouchers);

        bookingRepository.delete(booking);

        return ResponseEntity.ok("Xóa đặt phòng thành công!");
    }

    // Hàm hủy đặt phòng cho (user) khi status đặt phòng còn pending
    @Transactional
    public ResponseEntity<String> cancelBookingWhenStatusPending(UUID idBooking)
    {
        Booking booking = bookingRepository.findById(idBooking)
                .orElseThrow(() -> new ApiException(ErrorCode.BOOKING_NOT_FOUND));

        // Kiểm tra status booking còn Pending ko
        if (!PredefinedStatusBooking.STATUS_BOOKING_PENDING.equals(booking.getStatus())) {
            throw new ApiException(ErrorCode.BOOKING_NOT_CANCEL);
        }

        // Lấy danh sách booking room và booking service tương ứng
        List<BookingRoom> listBookingRoom = bookingRoomRepository.findByBooking_IdBooking(idBooking);
        List<BookingService> listBookingService = bookingServiceRepository.findByBooking_IdBooking(idBooking);

        if (!listBookingRoom.isEmpty())
        {
            // Gom nhóm số lượng room đặt theo từng booking room
            Map<UUID, Integer> restoreQuantityRooms = listBookingRoom.stream()
                    .collect(Collectors.groupingBy(
                            br -> br.getRoom().getIdRoom(),
                            Collectors.summingInt(BookingRoom::getQuantity)
                    ));

            // Hoàn lại số lượng cho room và cập nhật status nếu số lượng lớn hơn 0
            restoreQuantityRooms.forEach((idRoom, quantity) -> {
                roomRepository.restoreRoomQuantity(idRoom, quantity);
                roomRepository.updateStatusIfAvailable(idRoom, PredefinedStatusRoom.STATUS_ROOM_AVAILABLE);
            });
        }

        if (!listBookingService.isEmpty())
        {
            // Gom nhóm số lượng service đặt theo từng booking service
            Map<UUID, Integer> restoreQuantityServices = listBookingService.stream()
                    .collect(Collectors.groupingBy(
                            bs -> bs.getService().getIdService(),
                            Collectors.summingInt(BookingService::getQuantity)
                    ));

            // Hoàn lại số lượng cho service và cập nhật status nếu số lượng lớn hơn 0
            restoreQuantityServices.forEach((idService, quantity) -> {
                serviceRepository.restoreServiceQuantity(idService, quantity);
                serviceRepository.updateStatusIfAvailable(idService, PredefinedStatusService.STATUS_SERVICE_AVAILABLE);
            });
        }

        // Cập nhật trạng thái booking room thành Failed
        List<UUID> idRooms = bookingRoomRepository.findRoomIdsByBookingId(idBooking);
        if (!idRooms.isEmpty()) {
            roomRepository.updateStatusForRoomIds(idRooms, PredefinedStatusRoom.STATUS_ROOM_AVAILABLE);
        }

        // Cập nhật trạng thái booking service thành Failed
        List<UUID> idServices = bookingServiceRepository.findServiceIdsByBookingId(idBooking);
        if (!idServices.isEmpty()) {
            serviceRepository.updateStatusForServiceIds(idServices, PredefinedStatusService.STATUS_SERVICE_AVAILABLE);
        }

        booking.setStatus(PredefinedStatusBooking.STATUS_BOOKING_FAILED); // Cập nhật trạng thái booking thành Failed

        // Cập nhật trạng thái Payment thành Failed
        Payment payment = paymentRepository.findByBooking_IdBooking(idBooking);
        if (payment == null) {
            throw new ApiException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        payment.setStatus(PredefinedStatusPayment.STATUS_PAYMENT_FAILED);

        // Reset lại is_used và usedAt của voucher còn hạn
        List<UserVoucher> usedVouchers = userVoucherRepository.findUsedVouchersByBookingId(booking.getIdBooking());
        usedVouchers.forEach(uv -> {
            uv.setIsUsed(false);
            uv.setUsedAt(null);
        });
        userVoucherRepository.saveAll(usedVouchers);

        bookingRepository.save(booking);

        return ResponseEntity.ok("Hủy đặt phòng thành công!");
    }
}