package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.TypeServiceRequest;
import com.example.java_service.Dto.Response.TypeServiceResponse;
import com.example.java_service.Models.TypeService;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TypeServiceMapper
{
    TypeService toTypeService(TypeServiceRequest request);

    TypeServiceResponse toTypeServiceResponse(TypeService typeService);
}
