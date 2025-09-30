package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.UserCreationRequest;
import com.example.java_service.Dto.Request.UserUpdateRequest;
import com.example.java_service.Dto.Response.OwnerHotelResponse;
import com.example.java_service.Dto.Response.UserResponse;
import com.example.java_service.Models.Users;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper
{
    @Mapping(target = "passwordUser", ignore = true)
    @Mapping(target = "role", ignore = true)
    Users toUser(UserCreationRequest request);

    @Mapping(target = "role", ignore = true)
    @Mapping(target = "typeUser", ignore = true)
    UserResponse toUserResponse(Users user);

    OwnerHotelResponse toOwnerHotelResponse(Users user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "typeUser", ignore = true)
    void updateUserFromRequest(UserUpdateRequest request, @MappingTarget Users users);
}
