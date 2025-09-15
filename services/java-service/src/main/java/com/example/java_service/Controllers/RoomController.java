package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.RoomCreationRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.RoomCreationResponse;
import com.example.java_service.Services.RoomService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<RoomCreationResponse> creationRoom(@RequestBody RoomCreationRequest request)
    {
        return ApiResponse.<RoomCreationResponse>builder()
                .message("Tạo phòng thành công!")
                .data(roomService.saveRoom(request))
                .build();
    }}
