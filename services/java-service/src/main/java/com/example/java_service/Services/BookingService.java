package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedStatusBooking;
import com.example.java_service.Constant.PredefinedStatusRoom;
import com.example.java_service.Dto.Request.BookingRequest;
import com.example.java_service.Dto.Request.BookingRoomRequest;
import com.example.java_service.Dto.Request.BookingUpdateRequest;
import com.example.java_service.Dto.Request.PaymentRequest;
import com.example.java_service.Dto.Response.BookingResponse;
import com.example.java_service.Dto.Response.BookingRoomResponse;
import com.example.java_service.Dto.Response.BookingServiceResponse;
import com.example.java_service.Dto.Response.PaymentResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.BookingMapper;
import com.example.java_service.Models.Booking;
import com.example.java_service.Models.Room;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingService
{
    BookingRepository bookingRepository;
    BookingRoomRepository bookingRoomRepository;
    BookingServiceRepository bookingServiceRepository;
    PaymentRepository paymentRepository;
    RoomRepository roomRepository;
    UserRepository userRepository;
    BookingMapper bookingMapper;
    BookingRoomService bookingRoomService;
    BookingServiceService bookingServiceService;
    WebClient webClient;

    // Hàm lấy danh sách đặt phòng của khách sạn (cho company)

    // Hàm lấy lịch sử đặt phòng của người dùng (cho user)

    // Hàm lấy chi tiết thông tin đặt phòng

    // Hàm tạo đặt phòng
    public PaymentResponse saveBooking(BookingRequest bookingRequest)
    {
        Users user = userRepository.findById(bookingRequest.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Booking booking = bookingMapper.toBooking(bookingRequest);
        booking.setStatus(PredefinedStatusBooking.STATUS_BOOKING_DEFAULT);
        booking.setUsers(user);
        Booking savedBooking = bookingRepository.save(booking);

        BookingRoomResponse bookingRoomResponse = bookingRoomService.saveBookingRoom(bookingRequest.getBookingRooms(),
                                                                                        savedBooking.getIdBooking());
        BookingServiceResponse bookingServiceResponse = bookingServiceService.saveBookingService(bookingRequest.getBookingServices(),
                                                                                                    savedBooking.getIdBooking());

        // Lưu tổng tiền của lần booking
        booking.setTotalPrice(bookingServiceResponse.getPriceBookingService().add(bookingRoomResponse.getPriceBookingRoom()));
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
        if (paymentRepository.existsByBooking_IdBookingAndStatus(idBooking, "Success")){
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

    // Hàm xóa đặt phòng cho (company)

    // Hàm hủy đặt phòng cho (user) khi status đặt phòng còn pending
}