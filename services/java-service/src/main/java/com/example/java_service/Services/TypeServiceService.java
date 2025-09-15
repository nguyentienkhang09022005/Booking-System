package com.example.java_service.Services;

import com.example.java_service.Dto.Request.TypeServiceRequest;
import com.example.java_service.Dto.Response.TypeServiceResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.TypeServiceMapper;
import com.example.java_service.Models.TypeService;
import com.example.java_service.Repositories.Jpa.TypeServiceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TypeServiceService
{
    TypeServiceRepository typeServiceRepository;
    TypeServiceMapper typeServiceMapper;

    // Hàm lấy danh sách loại dịch vụ
    public List<TypeService> getAllTypeServices()
    {
        return typeServiceRepository.findAll();
    }

    // Hàm tạo loại dịch vụ mới
    public TypeServiceResponse saveTypeService(TypeServiceRequest request)
    {
        if (typeServiceRepository.findById(request.getTypeService()).isPresent()) {
            throw new ApiException(ErrorCode.TYPE_SERVICE_EXISTS);
        }
        TypeService typeService = typeServiceRepository.save(typeServiceMapper.toTypeService(request));
        return typeServiceMapper.toTypeServiceResponse(typeService);
    }

    // Hàm xóa loại dịch vụ
    public ResponseEntity<String> deleteTypeService(String typeServiceName)
    {
        TypeService typeService = typeServiceRepository.findById(typeServiceName).orElseThrow(
                () -> new ApiException(ErrorCode.TYPE_SERVICE_NOT_FOUND)
        );
        typeServiceRepository.delete(typeService);
        return ResponseEntity.ok("Xóa loại dịch vụ thành công!");
    }
}
