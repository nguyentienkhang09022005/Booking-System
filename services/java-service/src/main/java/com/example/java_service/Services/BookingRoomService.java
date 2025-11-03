package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedStatusRoom;
import com.example.java_service.Dto.Request.BookingRoomRequest;
import com.example.java_service.Dto.Response.BookingRoomResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.BookingRoomMapper;
import com.example.java_service.Models.Booking;
import com.example.java_service.Models.BookingRoom;
import com.example.java_service.Models.Room;
import com.example.java_service.Repositories.Jpa.BookingRepository;
import com.example.java_service.Repositories.Jpa.BookingRoomRepository;
import com.example.java_service.Repositories.Jpa.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingRoomService
{
    BookingRoomRepository bookingRoomRepository;
    BookingRepository bookingRepository;
    RoomRepository roomRepository;
    BookingRoomMapper bookingRoomMapper;

    // Hàm đặt phòng
    public BookingRoomResponse saveBookingRoom(List<BookingRoomRequest> bookingRoomRequest, UUID idBooking)
    {
        if (bookingRoomRequest == null || bookingRoomRequest.isEmpty()){
            return null;
        }

        Booking booking = bookingRepository.findById(idBooking).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        // Lấy tất cả idRoom từ request
        List<UUID> idRooms = bookingRoomRequest.stream()
                .map(BookingRoomRequest::getIdRoom)
                .toList();
        List<Room> rooms = roomRepository.findAllById(idRooms);
        if (rooms.size() != idRooms.size()){
            throw new ApiException(ErrorCode.ROOM_NOT_FOUND);
        }

        // Xữ lý Race Condition bằng Atomistic
        for (BookingRoomRequest request : bookingRoomRequest)
        {
            UUID idRoom = request.getIdRoom();
            int quantity = request.getQuantity();

            int roomUpdated = roomRepository.decreaseRoomQuantity(idRoom, quantity);
            if (roomUpdated == 0)
                throw new ApiException(ErrorCode.ROOM_NOT_ENOUGH_QUANTITY);

            // Cập nhật tự động trạng thái phòng
            roomRepository.updateStatusIfOutOfRoom(idRoom, PredefinedStatusRoom.STATUS_ROOM_FULL);
        }

        List<BookingRoom> bookingRooms = bookingRoomRequest
                .stream()
                .map(request -> {
                    BookingRoom bookingRoomMapping = bookingRoomMapper.toBookingRoom(request);
                    bookingRoomMapping.setBooking(booking);

                    // Tìm room tương ứng
                    Room room = rooms.stream()
                            .filter(r -> r.getIdRoom().equals(request.getIdRoom()))
                            .findFirst()
                            .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));

                    bookingRoomMapping.setRoom(room);

                    // giá = đơn giá * số lượng
                    BigDecimal price = room.getRoomPrice()
                            .multiply(BigDecimal.valueOf(request.getQuantity()));
                    bookingRoomMapping.setPriceBookingRoom(price);

                    return bookingRoomMapping;
                }).toList();

        bookingRoomRepository.saveAll(bookingRooms);

        // Tính tổng giá phòng
        BigDecimal totalPrice = bookingRooms
                .stream()
                .map(BookingRoom::getPriceBookingRoom)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BookingRoomResponse bookingRoomResponse = new BookingRoomResponse();
        bookingRoomResponse.setPriceBookingRoom(totalPrice);
        return bookingRoomResponse;
    }
}
