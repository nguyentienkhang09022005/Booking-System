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
import com.example.java_service.Repositories.Jpa.HotelRepository;
import com.example.java_service.Repositories.Jpa.RoomRepository;
import com.example.java_service.Repositories.Jpa.TypeRoomRepository;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    HotelRepository hotelRepository;
    ImagesRepository imagesRepository;
    TypeRoomRepository typeRoomRepository;
    RoomMapper roomMapper;

    // Hàm lấy danh sách phòng bằng paging 8
    public Page<GetRoomResponse> getListRoomByPaging(UUID idHotel, Pageable pageable)
    {
        hotelRepository.findById(idHotel).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );

        Page<Room> roomPage = roomRepository.findAll(pageable);
        List<Room> roomsOnPage = roomPage.getContent();
        if (roomsOnPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // Lấy ID của tất cả phòng trong trang hiện tại
        List<String> roomIdsOnPage = roomsOnPage.stream()
                .map(room -> room.getIdRoom().toString())
                .toList();

        // Lấy tất cả ảnh của các room trên trang
        List<Images> images = imagesRepository.findByRefIdIn(roomIdsOnPage);

        // Gom nhóm ảnh theo idRoom
        Map<String, List<String>> roomImagesMap = images.stream()
                .collect(Collectors.groupingBy(
                        Images::getRefId,
                        Collectors.mapping(Images::getUrl, Collectors.toList())
                ));

        List<GetRoomResponse> responses = roomsOnPage.stream()
                .map(room -> {
                    GetRoomResponse response = roomMapper.toGetRoomResponse(room);
                    String roomIdStr = room.getIdRoom().toString();

                    response.setImageRoomUrl(roomImagesMap.get(roomIdStr));
                    return response;
                }).toList();

        return new PageImpl<>(responses, pageable, roomPage.getTotalElements());
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
    public RoomResponse saveRoom(RoomCreationRequest request)
    {
        Hotel hotel = hotelRepository.findById(request.getIdHotel()).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );

        Room room = roomMapper.toRoom(request);
        room.setHotel(hotel);
        room.setTypeRoom(typeRoomRepository.getReferenceById(request.getTypeRoom()));
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
        var roomResponse = roomMapper.toRoomResponse(savedRoom);
        return roomResponse;
    }

    // Hàm sửa thông tin phòng
    public RoomResponse updateRoom(RoomUpdateRequest request, UUID idRoom)
    {
        Room room = roomRepository.findById(idRoom).orElseThrow(
                () -> new ApiException(ErrorCode.ROOM_NOT_FOUND)
        );

        roomMapper.updateRoomFromRequest(request, room);
        room.setTypeRoom(typeRoomRepository.findById(request.getTypeRoom()).orElseThrow(
                () -> new ApiException(ErrorCode.TYPE_ROOM_NOT_FOUND)
        ));
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
