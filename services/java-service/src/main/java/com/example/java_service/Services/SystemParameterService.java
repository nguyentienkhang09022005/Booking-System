package com.example.java_service.Services;

import com.example.java_service.Dto.Request.SystemParameterUpdateRequest;
import com.example.java_service.Dto.Response.SystemParameterResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.SystemParameterMapper;
import com.example.java_service.Models.SystemParameter;
import com.example.java_service.Repositories.Jpa.SystemParameterRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemParameterService
{
    SystemParameterRepository systemParameterRepository;
    SystemParameterMapper systemParameterMapper;

    public List<SystemParameterResponse> getAllSystemParameters() {
        return systemParameterRepository.findAll().stream()
                .map(systemParameterMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SystemParameterResponse updateSystemParameter(String paramKey, SystemParameterUpdateRequest request) {
        SystemParameter systemParameter = systemParameterRepository.findById(paramKey).orElseThrow(
                () -> new ApiException(ErrorCode.INVALID_DATA) // Bạn có thể tạo thêm ErrorCode.PARAMETER_NOT_FOUND nếu cần
        );

        systemParameterMapper.updateSystemParameterFromRequest(request, systemParameter);
        SystemParameter updatedParam = systemParameterRepository.save(systemParameter);

        return systemParameterMapper.toResponse(updatedParam);
    }

    public String getParamValue(String paramKey, String defaultValue) {
        return systemParameterRepository.findById(paramKey)
                .map(SystemParameter::getParamValue)
                .orElse(defaultValue);
    }
}