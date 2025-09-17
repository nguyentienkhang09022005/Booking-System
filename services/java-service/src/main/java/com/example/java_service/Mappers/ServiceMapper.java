package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.ServiceCreationRequest;
import com.example.java_service.Dto.Request.ServiceUpdateRequest;
import com.example.java_service.Dto.Response.ServiceResponse;
import com.example.java_service.Models.Service;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ServiceMapper
{
    @Mapping(target = "typeService", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    Service toService(ServiceCreationRequest request);

    ServiceResponse toServiceResponse(Service service);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "typeService", ignore = true)
    void updateServiceFromRequest(ServiceUpdateRequest request, @MappingTarget Service service);
}
