package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.UserCreationRequest;
import com.example.java_service.Dto.Request.UserUpdateRequest;
import com.example.java_service.Models.Users;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper
{
    @Mapping(target = "passwordUser", ignore = true)
    Users toUser(UserCreationRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromRequest(UserUpdateRequest request, @MappingTarget Users users);
}
