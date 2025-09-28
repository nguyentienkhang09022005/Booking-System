package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.RoomCreationRequest;
import com.example.java_service.Dto.Request.RoomUpdateRequest;
import com.example.java_service.Dto.Response.*;
import com.example.java_service.Services.RoomService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/room")
public class RoomController
{
    RoomService roomService;

    // Endpoint lấy danh sách phòng
    @GetMapping("/getListRoom")
    public ApiResponse<List<GetRoomResponse>> getRooms(@RequestParam UUID idHotel)
    {
        return ApiResponse.<List<GetRoomResponse>>builder()
                .message("Lấy danh sách phòng thành công!")
                .data(roomService.getListRoom(idHotel))
                .build();
    }

    // Endpoint lấy thông tin chi tiết phòng
    @GetMapping( "/getInfRoom")
    public ApiResponse<GetRoomResponse> getInfRoom(@RequestParam UUID idRoom)
    {
        return ApiResponse.<GetRoomResponse>builder()
                .message("Lấy thông tin chi tiết phòng thành công!")
                .data(roomService.getInfRoom(idRoom))
                .build();
    }

    // Endpoint tạo phòng mới
    @PostMapping("/creation")
    public ApiResponse<RoomResponse> creationRoom(@RequestBody RoomCreationRequest request)
    {
        return ApiResponse.<RoomResponse>builder()
                .message("Tạo phòng thành công!")
                .data(roomService.saveRoom(request))
                .build();
    }

    // Endpoint cập nhật phòng
    @PatchMapping( "/update")
    public ApiResponse<RoomResponse> updateRoom(@RequestParam UUID idRoom,
                                                @RequestBody RoomUpdateRequest request)
    {
        return ApiResponse.<RoomResponse>builder()
                .message("Thay đổi thông tin phòng thành công!")
                .data(roomService.updateRoom(request, idRoom))
                .build();
    }

    // Endpoint xóa phòng
    @DeleteMapping( "/delele")
    public ApiResponse<ResponseEntity<String>> deleteRoom(@RequestParam UUID idRoom)
    {
        var response = roomService.deleteRoom(idRoom);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }
}

