package com.example.java_service.Services;

import com.example.java_service.Dto.Request.TypeRoomRequest;
import com.example.java_service.Dto.Response.TypeRoomResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.TypeRoomMapper;
import com.example.java_service.Models.TypeRoom;
import com.example.java_service.Repositories.Jpa.TypeRoomRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TypeRoomService
{
    TypeRoomRepository typeRoomRepository;
    TypeRoomMapper typeRoomMapper;

    // Hàm lấy danh sách loại phòng
    public List<TypeRoom> getAllTypeRooms()
    {
        return typeRoomRepository.findAll();
    }

    // Hàm tạo loại phòng mới
    public TypeRoomResponse saveTypeRoom(TypeRoomRequest request)
    {
        if (typeRoomRepository.findById(request.getTypeRoom()).isPresent()) {
            throw new ApiException(ErrorCode.TYPE_ROOM_EXISTS);
        }
        TypeRoom typeRoom = typeRoomRepository.save(typeRoomMapper.toTypeRoom(request));
        return typeRoomMapper.toTypeRoomResponse(typeRoom);
    }

    // Hàm xóa loại phòng
    public ResponseEntity<String> deleteTypeRoom(String typeRoomName)
    {
        TypeRoom typeRoom = typeRoomRepository.findById(typeRoomName).orElseThrow(
                () -> new ApiException(ErrorCode.TYPE_ROOM_NOT_FOUND)
        );
        typeRoomRepository.delete(typeRoom);
        return ResponseEntity.ok("Xóa loại phòng thành công!");
    }
}
