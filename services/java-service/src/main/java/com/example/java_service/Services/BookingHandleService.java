package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedStatusBooking;
import com.example.java_service.Constant.PredefinedStatusPayment;
import com.example.java_service.Constant.PredefinedStatusRoom;
import com.example.java_service.Constant.PredefinedStatusService;
import com.example.java_service.Dto.Request.*;
import com.example.java_service.Dto.Response.*;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.BookingMapper;
import com.example.java_service.Mappers.RoomMapper;
import com.example.java_service.Mappers.ServiceMapper;
import com.example.java_service.Models.*;
import com.example.java_service.Repositories.Jpa.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    HotelRepository hotelRepository;
    VoucherRepository voucherRepository;
    BookingMapper bookingMapper;
    RoomMapper roomMapper;
    ServiceMapper serviceMapper;
    BookingRoomService bookingRoomService;
    BookingServiceService bookingServiceService;
    UserVoucherService userVoucherService;
    AppliedVoucherService appliedVoucherService;
    WebClient webClient;

    // Hàm lấy danh sách đặt phòng của khách sạn (cho company)
    public List<BookingListResponse> getListBooking(UUID idHotel)
    {
        hotelRepository.findById(idHotel).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );

        // Lấy tất cả danh sách booking
        List<Booking> bookingList = bookingRepository.findByHotel_IdHotel(idHotel);
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
    @Transactional
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

        return response;
    }


    // Hàm tạo đặt phòng
    public PaymentResponse saveBooking(BookingRequest bookingRequest)
    {
        Users user = userRepository.findById(bookingRequest.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Hotel hotel = hotelRepository.findById(bookingRequest.getIdHotel()).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );

        if (bookingRequest.getCheckoutDate().before(bookingRequest.getCheckinDate())){
            throw new ApiException(ErrorCode.CHECKOUT_DATE_BEFORE_CHECKIN_DATE);
        }

        Booking booking = bookingMapper.toBooking(bookingRequest);
        booking.setStatus(PredefinedStatusBooking.STATUS_BOOKING_PENDING);
        booking.setUsers(user);
        booking.setHotel(hotel);
        Booking savedBooking = bookingRepository.save(booking);

        BookingRoomResponse bookingRoomResponse = bookingRoomService.saveBookingRoom(bookingRequest.getBookingRooms(),
                                                                                        savedBooking.getIdBooking());
        BookingServiceResponse bookingServiceResponse = bookingServiceService.saveBookingService(bookingRequest.getBookingServices(),
                                                                                                    savedBooking.getIdBooking());

        // Lưu tổng tiền của lần booking khi chưa có voucher
        BigDecimal totalRoom = BigDecimal.ZERO;
        BigDecimal totalService = BigDecimal.ZERO;
        if (bookingRoomResponse != null && bookingRoomResponse.getPriceBookingRoom() != null) {
            totalRoom = bookingRoomResponse.getPriceBookingRoom();
        }

        if (bookingServiceResponse != null && bookingServiceResponse.getPriceBookingService() != null) {
            totalService = bookingServiceResponse.getPriceBookingService();
        }

        BigDecimal totalPrice = totalRoom.add(totalService);

        // Xữ lý nếu có voucher
        if (bookingRequest.getUserVoucherRequests() != null && !bookingRequest.getUserVoucherRequests().isEmpty()) {
            for (UserVoucherRequest userVoucherRequest : bookingRequest.getUserVoucherRequests())
            {
                // Lưu voucher
                userVoucherService.saveUserVoucherForBooking(userVoucherRequest);

                // Lưu voucher đã sử dụng
                AppliedVoucherRequest appliedVoucherRequest = AppliedVoucherRequest.builder()
                        .idUser(userVoucherRequest.getIdUser())
                        .idVoucher(userVoucherRequest.getIdVoucher())
                        .build();
                appliedVoucherService.saveAppliedVoucher(appliedVoucherRequest, savedBooking.getIdBooking());

                Voucher voucher = voucherRepository.findById(userVoucherRequest.getIdVoucher())
                        .orElseThrow(() -> new ApiException(ErrorCode.VOUCHER_NOT_FOUND));

                if (voucher.getDiscountPercent() != null && voucher.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal discount = totalPrice
                            .multiply(voucher.getDiscountPercent())
                            .divide(BigDecimal.valueOf(100), 10, RoundingMode.DOWN);
                    totalPrice = totalPrice.subtract(discount);
                }
            }
        }

        booking.setTotalPrice(totalPrice);
        bookingRepository.save(booking);

        // Call api thanh toán
        PaymentRequest paymentRequest = new PaymentRequest(
                booking.getUsers().getIdUser(),
                savedBooking.getIdBooking(),
                booking.getTotalPrice().doubleValue()
        );
        try
        {
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
        catch (Exception e) {
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
        BigDecimal totalRoom = BigDecimal.ZERO;
        BigDecimal totalService = BigDecimal.ZERO;
        if (bookingRoomResponse != null && bookingRoomResponse.getPriceBookingRoom() != null) {
            totalRoom = bookingRoomResponse.getPriceBookingRoom();
        }

        if (bookingServiceResponse != null && bookingServiceResponse.getPriceBookingService() != null) {
            totalService = bookingServiceResponse.getPriceBookingService();
        }

        booking.setTotalPrice(totalRoom.add(totalService));
        bookingRepository.save(booking);

        paymentRepository.deleteByBooking_IdBooking(idBooking); // Xóa payment cũ

        // Call api thanh toán
        PaymentRequest paymentRequest = new PaymentRequest(
                booking.getUsers().getIdUser(),
                booking.getIdBooking(),
                booking.getTotalPrice().doubleValue()
        );
        try
        {
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
        catch (Exception e) {
            booking.setStatus(PredefinedStatusBooking.STATUS_BOOKING_FAILED);
            bookingRepository.save(booking);
            throw new ApiException(ErrorCode.BOOKING_FAILED);
        }
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

        bookingRepository.save(booking);

        return ResponseEntity.ok("Hủy đặt phòng thành công!");
    }
}