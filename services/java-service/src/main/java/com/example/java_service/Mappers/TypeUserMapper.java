package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.TypeUserRequest;
import com.example.java_service.Dto.Response.TypeUserResponse;
import com.example.java_service.Models.TypeUser;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TypeUserMapper
{
    TypeUser toTypeUser(TypeUserRequest request);

    TypeUserResponse toTypeUserResponse(TypeUser typeUser);
}
