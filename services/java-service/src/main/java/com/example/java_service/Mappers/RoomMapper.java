package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.RoomCreationRequest;
import com.example.java_service.Dto.Request.RoomUpdateRequest;
import com.example.java_service.Dto.Response.RoomResponse;
import com.example.java_service.Models.Room;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RoomMapper
{
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "typeRoom", ignore = true)
    Room toRoom(RoomCreationRequest request);

    RoomResponse toRoomCreationResponse(Room room);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "typeRoom", ignore = true)
    void updateRoomFromRequest(RoomUpdateRequest request, @MappingTarget Room room);
}
