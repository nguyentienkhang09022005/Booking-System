package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.HotelUpdateRequest;
import com.example.java_service.Dto.Request.RoomCreationRequest;
import com.example.java_service.Dto.Request.RoomUpdateRequest;
import com.example.java_service.Dto.Response.*;
import com.example.java_service.Services.RoomService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/room")
public class RoomController
{
    RoomService roomService;

    // Endpoint lấy danh sách phòng theo paging 8
    @GetMapping("/getListRoom")
    public ApiResponse<Page<GetRoomResponse>> getRooms(@RequestParam UUID idHotel,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "8") int size)
    {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.<Page<GetRoomResponse>>builder()
                .message("Lấy danh sách phòng thành công!")
                .data(roomService.getListRoomByPaging(idHotel, pageable))
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

