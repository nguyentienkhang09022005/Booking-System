package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.RoomCreationRequest;
import com.example.java_service.Dto.Response.RoomCreationResponse;
import com.example.java_service.Models.Room;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RoomMapper
{
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "typeRoom", ignore = true)
    Room toRoom(RoomCreationRequest request);

    RoomCreationResponse toRoomCreationResponse(Room room);
}
