package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedStatusService;
import com.example.java_service.Dto.Request.ServiceCreationRequest;
import com.example.java_service.Dto.Request.ServiceUpdateRequest;
import com.example.java_service.Dto.Response.ServiceResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.ServiceMapper;
import com.example.java_service.Models.Hotel;
import com.example.java_service.Models.Service;
import com.example.java_service.Repositories.Jpa.HotelRepository;
import com.example.java_service.Repositories.Jpa.ServiceRepository;
import com.example.java_service.Repositories.Jpa.TypeServiceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServiceService
{
    ServiceRepository serviceRepository;
    HotelRepository hotelRepository;
    TypeServiceRepository typeServiceRepository;
    ServiceMapper serviceMapper;

    // Hàm lấy danh sách dịch vụ
    public List<ServiceResponse> getListService(UUID idHotel)
    {
        hotelRepository.findById(idHotel).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );

        List<Service> servicesList = serviceRepository.findByHotel_IdHotel(idHotel);
        if (servicesList.isEmpty()) {
            return List.of();
        }

        return servicesList.stream()
                .map(service -> {
                    ServiceResponse response = serviceMapper.toServiceResponse(service);
                    return response;
                }).toList();
    }

    // Hàm lấy thông tin chi tiết dịch vụ

    // Hàm thêm dịch vụ mới
    public ServiceResponse saveService(ServiceCreationRequest request)
    {
        Hotel hotel = hotelRepository.findById(request.getIdHotel()).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );
        Service service = serviceMapper.toService(request);
        service.setHotel(hotel);
        service.setTypeService(typeServiceRepository.findById(request.getTypeService()).orElseThrow(
                () -> new ApiException(ErrorCode.TYPE_SERVICE_NOT_FOUND)
        ));
        service.setStatus(PredefinedStatusService.STATUS_SERVICE_AVAILABLE);
        Service savedService = serviceRepository.save(service);

        return serviceMapper.toServiceResponse(savedService);
    }

    // Hàm sửa thông tin dịch vụ
    public ServiceResponse updateService(ServiceUpdateRequest request, UUID idService)
    {
        Service service = serviceRepository.findById(idService).orElseThrow(
                () -> new ApiException(ErrorCode.SERVICE_NOT_FOUND)
        );
        serviceMapper.updateServiceFromRequest(request, service);
        service.setTypeService(typeServiceRepository.findById(request.getTypeService()).orElseThrow(
                () -> new ApiException(ErrorCode.TYPE_SERVICE_NOT_FOUND)
        ));
        Service savedService = serviceRepository.save(service);

        return serviceMapper.toServiceResponse(savedService);
    }

    // Hàm xóa dịch vụ
    public ResponseEntity<String> deleteService(UUID idService)
    {
        Service service = serviceRepository.findById(idService).orElseThrow(
                () -> new ApiException(ErrorCode.SERVICE_NOT_FOUND)
        );
        serviceRepository.delete(service);
        return ResponseEntity.ok("Xóa dịch vụ thành công!");
    }
}
