package com.example.Booking_Hotel_BE.Mappers;

import com.example.Booking_Hotel_BE.Dto.Request.RegisterRequest;
import com.example.Booking_Hotel_BE.Dto.Response.UserResponse;
import com.example.Booking_Hotel_BE.Models.Users;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper
{
    @Mapping(target = "passwordUser", ignore = true)
    Users toUser(RegisterRequest request);

    UserResponse toUserResponse(Users user);
}
