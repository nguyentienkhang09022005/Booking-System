package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.EvaluateCreationRequest;
import com.example.java_service.Dto.Request.EvaluateUpdateRequest;
import com.example.java_service.Dto.Response.EvaluateResponse;
import com.example.java_service.Models.Evaluates;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EvaluateMapper
{
    @Mapping(target = "createdAt", ignore = true)
    Evaluates toEvaluate(EvaluateCreationRequest request);

    EvaluateResponse toEvaluateResponse(Evaluates evaluates);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEvaluateFromRequest(EvaluateUpdateRequest request, @MappingTarget Evaluates evaluates);
}
