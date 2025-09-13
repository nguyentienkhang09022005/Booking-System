package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.RoleRequest;
import com.example.java_service.Dto.Response.RoleResponse;
import com.example.java_service.Models.Roles;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper
{
    Roles toRole(RoleRequest request);

    RoleResponse toRoleResponse(Roles role);
}
