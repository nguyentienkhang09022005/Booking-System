package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedStatusBooking;
import com.example.java_service.Dto.Request.BookingRequest;
import com.example.java_service.Dto.Request.PaymentRequest;
import com.example.java_service.Dto.Response.BookingRoomResponse;
import com.example.java_service.Dto.Response.BookingServiceResponse;
import com.example.java_service.Dto.Response.PaymentResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.BookingMapper;
import com.example.java_service.Models.Booking;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.BookingRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingService
{
    BookingRepository bookingRepository;
    UserRepository userRepository;
    BookingMapper bookingMapper;
    BookingRoomService bookingRoomService;
    BookingServiceService bookingServiceService;
    WebClient webClient;

    // Hàm lấy danh sách đặt phòng của khách sạn (cho company)

    // Hàm lấy lịch sử đặt phòng của người dùng (cho user)

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
}