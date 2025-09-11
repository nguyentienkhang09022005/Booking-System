package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.HotelCreationRequest;
import com.example.java_service.Dto.Request.HotelUpdateRequest;
import com.example.java_service.Dto.Request.UserCreationRequest;
import com.example.java_service.Dto.Response.GetListHotelResponse;
import com.example.java_service.Dto.Response.HotelCreationResponse;
import com.example.java_service.Models.Hotel;
import com.example.java_service.Models.Users;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper
{
    @Mapping(target = "passwordUser", ignore = true)
    Users toUser(UserCreationRequest request);
}
