package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedStatusBooking;
import com.example.java_service.Constant.PredefinedStatusPayment;
import com.example.java_service.Constant.PredefinedStatusRoom;
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
    UserRepository userRepository;
    HotelRepository hotelRepository;
    BookingMapper bookingMapper;
    RoomMapper roomMapper;
    ServiceMapper serviceMapper;
    BookingRoomService bookingRoomService;
    BookingServiceService bookingServiceService;
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
        List<BookingRoom> bookingRooms = bookingRoomRepository.findAllByBookingIds(List.of(idBooking));
        List<BookingService> bookingServices = bookingServiceRepository.findAllByBookingIds(List.of(idBooking));
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

        Booking booking = bookingMapper.toBooking(bookingRequest);
        booking.setStatus(PredefinedStatusBooking.STATUS_BOOKING_DEFAULT);
        booking.setUsers(user);
        booking.setHotel(hotel);
        Booking savedBooking = bookingRepository.save(booking);

        BookingRoomResponse bookingRoomResponse = bookingRoomService.saveBookingRoom(bookingRequest.getBookingRooms(),
                                                                                        savedBooking.getIdBooking());
        BookingServiceResponse bookingServiceResponse = bookingServiceService.saveBookingService(bookingRequest.getBookingServices(),
                                                                                                    savedBooking.getIdBooking());

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
    public ResponseEntity<String> updateBooking(BookingUpdateRequest bookingUpdateRequest, UUID idBooking)
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
            bookingRoomRepository.deleteByBooking_IdBooking(idBooking);
            // Lấy tất cả idRoom từ request
            List<UUID> idRooms = bookingUpdateRequest.getBookingRooms().stream()
                    .map(BookingRoomRequest::getIdRoom)
                    .toList();
            List<Room> rooms = roomRepository.findAllById(idRooms);
            if (rooms.size() != idRooms.size()){
                throw new ApiException(ErrorCode.ROOM_NOT_FOUND);
            }

            for (UUID idRoom : idRooms)
            {
                int updateStatusRoom = roomRepository.updateStatus(
                        idRoom,
                        PredefinedStatusRoom.STATUS_ROOM_BOOKED,
                        PredefinedStatusRoom.STATUS_ROOM_DEFAULT
                );
                if (updateStatusRoom == 0) {
                    throw new ApiException(ErrorCode.ROOM_ALREADY_BOOKED);
                }
            }
            // Cập nhật trạng thái cho toàn bộ room
            rooms.forEach(room -> room.setStatus(PredefinedStatusRoom.STATUS_ROOM_BOOKED));
            roomRepository.saveAll(rooms);
            bookingRoomResponse = bookingRoomService.saveBookingRoom(bookingUpdateRequest.getBookingRooms(),
                                                                                         idBooking);
        }

        // Nếu có thay đổi thông tin đặt dịch vụ
        BookingServiceResponse bookingServiceResponse = null;
        if (bookingUpdateRequest.getBookingServices() != null && !bookingUpdateRequest.getBookingServices().isEmpty())
        {
            bookingServiceRepository.deleteByBooking_IdBooking(idBooking);
            bookingServiceResponse = bookingServiceService.saveBookingService(bookingUpdateRequest.getBookingServices(),
                                                                                                     idBooking);
        }

        // Lưu lại tổng tiền của lần booking
        booking.setTotalPrice(bookingServiceResponse.getPriceBookingService().add(bookingRoomResponse.getPriceBookingRoom()));
        bookingRepository.save(booking);

        return ResponseEntity.ok("Thay đổi thông tin đặt phòng thành công!");
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

        // Lấy danh idRoom sách tất cả BookingRoom của Booking
        List<UUID> roomIds = bookingRoomRepository.findRoomIdsByBookingId(idBooking);
        if (!roomIds.isEmpty()) {
            roomRepository.updateStatusForRoomIds(roomIds, PredefinedStatusRoom.STATUS_ROOM_DEFAULT);
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

        if (!PredefinedStatusBooking.STATUS_BOOKING_DEFAULT.equals(booking.getStatus())) {
            throw new ApiException(ErrorCode.BOOKING_NOT_CANCEL);
        }

        List<UUID> roomIds = bookingRoomRepository.findRoomIdsByBookingId(idBooking);
        if (!roomIds.isEmpty()) {
            roomRepository.updateStatusForRoomIds(roomIds, PredefinedStatusRoom.STATUS_ROOM_DEFAULT);
        }
        booking.setStatus(PredefinedStatusBooking.STATUS_BOOKING_FAILED);

        // Cập nhật trạng thái Payment
        Payment payment = paymentRepository.findByBooking_IdBooking(idBooking);
        if (payment == null) {
            throw new ApiException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        payment.setStatus(PredefinedStatusPayment.STATUS_PAYMENT_FAILED);

        bookingRepository.save(booking);

        return ResponseEntity.ok("Hủy đặt phòng thành công!");
    }
}