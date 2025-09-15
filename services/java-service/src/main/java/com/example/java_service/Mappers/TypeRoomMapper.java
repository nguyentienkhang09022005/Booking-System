package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.TypeRoomRequest;
import com.example.java_service.Dto.Response.TypeRoomResponse;
import com.example.java_service.Models.TypeRoom;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TypeRoomMapper
{
    TypeRoom toTypeRoom(TypeRoomRequest request);

    TypeRoomResponse toTypeRoomResponse(TypeRoom typeRoom);
}
