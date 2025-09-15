package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.TypeRoomRequest;
import com.example.java_service.Dto.Request.TypeServiceRequest;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.TypeRoomResponse;
import com.example.java_service.Dto.Response.TypeServiceResponse;
import com.example.java_service.Models.TypeRoom;
import com.example.java_service.Models.TypeService;
import com.example.java_service.Services.TypeRoomService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/typeRoom")
public class TypeRoomController
{
    TypeRoomService typeRoomService;

    // Endpoint lấy danh sách loại phòng
    @GetMapping( "/list")
    public ApiResponse<List<TypeRoom>> getListTypeRoom()
    {
        return ApiResponse.<List<TypeRoom>>builder()
                .message("Lấy danh sách loại phòng thành công!")
                .data(typeRoomService.getAllTypeRooms())
                .build();
    }

    // Endpoint tạo loại phòng
    @PostMapping( "/creation")
    public ApiResponse<TypeRoomResponse> createTypeRoom(@RequestBody TypeRoomRequest request)
    {
        return ApiResponse.<TypeRoomResponse>builder()
                .message("Tạo loại phòng thành công!")
                .data(typeRoomService.saveTypeRoom(request))
                .build();
    }

    // Endpoint xóa loại phòng
    @DeleteMapping( "/delete")
    public ApiResponse<ResponseEntity<String>> deleteTypeRoom(@RequestParam String typeRoom)
    {
        var response = typeRoomService.deleteTypeRoom(typeRoom);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }
}
