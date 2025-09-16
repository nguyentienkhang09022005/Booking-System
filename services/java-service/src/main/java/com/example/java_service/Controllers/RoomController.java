package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.RoomCreationRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.RoomResponse;
import com.example.java_service.Services.RoomService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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

    // Endpoint tạo phòng mới
    @PostMapping("/creation")
    public ApiResponse<RoomResponse> creationRoom(@RequestBody RoomCreationRequest request)
    {
        return ApiResponse.<RoomResponse>builder()
                .message("Tạo phòng thành công!")
                .data(roomService.saveRoom(request))
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

