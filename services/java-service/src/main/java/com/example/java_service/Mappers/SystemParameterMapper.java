package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.SystemParameterUpdateRequest;
import com.example.java_service.Dto.Response.SystemParameterResponse;
import com.example.java_service.Models.SystemParameter;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SystemParameterMapper
{
    SystemParameterResponse toResponse(SystemParameter systemParameter);

    void updateSystemParameterFromRequest(SystemParameterUpdateRequest request, @MappingTarget SystemParameter systemParameter);
}