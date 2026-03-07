package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedStatusRoom;
import com.example.java_service.Dto.Request.RoomCreationRequest;
import com.example.java_service.Dto.Request.RoomUpdateRequest;
import com.example.java_service.Dto.Response.GetRoomResponse;
import com.example.java_service.Dto.Response.RoomResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.RoomMapper;
import com.example.java_service.Models.*;
import com.example.java_service.Repositories.Jpa.AccommodationRepository;
import com.example.java_service.Repositories.Jpa.RoomRepository;
import com.example.java_service.Repositories.Jpa.TypeRoomRepository;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomService
{
    RoomRepository roomRepository;
    AccommodationRepository accommodationRepository;
    ImagesRepository imagesRepository;
    TypeRoomRepository typeRoomRepository;
    RoomMapper roomMapper;

    // Hàm lấy danh sách phòng
    public List<GetRoomResponse> getListRoom(UUID idAccommodation)
    {
        accommodationRepository.findById(idAccommodation).orElseThrow(
                () -> new ApiException(ErrorCode.ACCOMMODATION_NOT_FOUND)
        );

        List<Room> roomList = roomRepository.findByAccommodation_IdAccommodation(idAccommodation);
        if (roomList.isEmpty()) {
            return List.of();
        }

        // Lấy ID của tất cả phòng
        List<String> roomIds = roomList.stream()
                .map(room -> room.getIdRoom().toString())
                .toList();

        // Lấy tất cả ảnh của các room
        List<Images> images = imagesRepository.findByRefIdIn(roomIds);

        // Gom nhóm ảnh theo idRoom
        Map<String, List<String>> roomImagesMap = images.stream()
                .collect(Collectors.groupingBy(
                        Images::getRefId,
                        Collectors.mapping(Images::getUrl, Collectors.toList())
                ));

        return roomList.stream()
                .map(room -> {
                    GetRoomResponse response = roomMapper.toGetRoomResponse(room);
                    String roomIdStr = room.getIdRoom().toString();

                    response.setImageRoomUrl(roomImagesMap.get(roomIdStr));
                    return response;
                }).toList();
    }

   // Hàm lấy thông tin chi tiết phòng
    public GetRoomResponse getInfRoom(UUID idRoom)
    {
        Room room = roomRepository.findById(idRoom).orElseThrow(
                () -> new ApiException(ErrorCode.ROOM_NOT_FOUND)
        );

        List<String> imageUrls = imagesRepository.findByRefId(room.getIdRoom().toString())
                .stream()
                .map(Images::getUrl)
                .toList();

        GetRoomResponse roomResponse = roomMapper.toGetRoomResponse(room);
        roomResponse.setImageRoomUrl(imageUrls);
        return roomResponse;
    }

    // Hàm thêm phòng mới
    @Transactional
    public ResponseEntity<String> saveRoom(RoomCreationRequest request)
    {
        Accommodation accommodation = accommodationRepository.findById(request.getIdAccommodation()).orElseThrow(
                () -> new ApiException(ErrorCode.ACCOMMODATION_NOT_FOUND)
        );

        Room room = roomMapper.toRoom(request);
        room.setAccommodation(accommodation);
        room.setTypeRoom(typeRoomRepository.getReferenceById(request.getTypeRoom()));
        room.setStatus(PredefinedStatusRoom.STATUS_ROOM_AVAILABLE);
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
        return ResponseEntity.ok("Tạo phòng thành công!");
    }

    // Hàm sửa thông tin phòng
    @Transactional
    public RoomResponse updateRoom(UUID idRoom, RoomUpdateRequest request)
    {
        Room room = roomRepository.findById(idRoom).orElseThrow(
                () -> new ApiException(ErrorCode.ROOM_NOT_FOUND)
        );

        // Kiểm tra TypeRoom
        if (request.getTypeRoom() != null) {
            TypeRoom typeRoom = typeRoomRepository.findById(request.getTypeRoom()).orElseThrow(
                    () -> new ApiException(ErrorCode.TYPE_ROOM_NOT_FOUND)
            );
            room.setTypeRoom(typeRoom);
        }
        roomMapper.updateRoomFromRequest(request, room);

        Room updatedRoom = roomRepository.save(room);

        // Nếu có upload ảnh mới
        if (request.getListImageRoom() != null && !request.getListImageRoom().isEmpty()) {
            imagesRepository.deleteByRefId(updatedRoom.getIdRoom().toString());

            List<Images> images = request.getListImageRoom().stream().map(url -> {
                Images image = new Images();
                image.setUrl(url);
                image.setRefId(updatedRoom.getIdRoom().toString());
                return image;
            }).toList();
            imagesRepository.saveAll(images);
        }

        var roomResponse = roomMapper.toRoomResponse(updatedRoom);
        roomResponse.setTypeRoom(updatedRoom.getTypeRoom());
        return roomResponse;
    }

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
