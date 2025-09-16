package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedStatusRoom;
import com.example.java_service.Dto.Request.RoomCreationRequest;
import com.example.java_service.Dto.Request.RoomUpdateRequest;
import com.example.java_service.Dto.Response.RoomResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.RoomMapper;
import com.example.java_service.Models.*;
import com.example.java_service.Repositories.Jpa.HotelRepository;
import com.example.java_service.Repositories.Jpa.RoomRepository;
import com.example.java_service.Repositories.Jpa.TypeRoomRepository;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomService
{
    RoomRepository roomRepository;
    HotelRepository hotelRepository;
    ImagesRepository imagesRepository;
    TypeRoomRepository typeRoomRepository;
    RoomMapper roomMapper;

    // Hàm lấy danh sách phòng bằng paging 8

    // Hàm lấy thông tin chi tiết phòng

    // Hàm thêm phòng mới
    public RoomResponse saveRoom(RoomCreationRequest request)
    {
        Hotel hotel = hotelRepository.findById(request.getIdHotel()).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );

        Room room = roomMapper.toRoom(request);
        room.setHotel(hotel);
        room.setTypeRoom(typeRoomRepository.findById(request.getTypeRoom()).orElseThrow(
                () -> new ApiException(ErrorCode.TYPE_ROOM_NOT_FOUND)
        ));
        room.setStatus(PredefinedStatusRoom.STATUS_ROOM_DEFAULT);
        Room savedRoom = roomRepository.save(room);

        // Lưu ảnh từ URL đã upload
        if (request.getListImageRoom() != null && !request.getListImageRoom().isEmpty()) {
            List<Images> images = request.getListImageRoom().stream().map(url -> {
                Images image = new Images();
                image.setUrl(url);
                image.setRefId(savedRoom.getIdRoom().toString());
                return image;
            }).toList();
            imagesRepository.saveAll(images);
        }
        var roomResponse = roomMapper.toRoomCreationResponse(savedRoom);
        return roomResponse;
    }

    // Hàm sửa thông tin phòng
//    public RoomResponse updateRoom(RoomUpdateRequest request, UUID idRoom)
//    {
//        Room room = roomRepository.findById(idRoom).orElseThrow(
//                () -> new ApiException(ErrorCode.ROOM_NOT_FOUND)
//        );
//
//        roomMapper.updateRoomFromRequest(request,room);
//
//    }

    // Hàm xóa phòng
    public ResponseEntity<String> deleteRoom(UUID idRoom)
    {
        Room room = roomRepository.findById(idRoom).orElseThrow(
                () -> new ApiException(ErrorCode.ROOM_NOT_FOUND)
        );
        imagesRepository.deleteByRefId(idRoom.toString());
        roomRepository.delete(room);
        return ResponseEntity.ok("Xóa phòng thành công!");
    }
}
