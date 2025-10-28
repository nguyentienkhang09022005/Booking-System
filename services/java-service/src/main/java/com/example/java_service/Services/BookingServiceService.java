package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedStatusService;
import com.example.java_service.Dto.Request.BookingServiceRequest;
import com.example.java_service.Dto.Response.BookingServiceResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.BookingServiceMapper;
import com.example.java_service.Models.Booking;
import com.example.java_service.Models.BookingService;
import com.example.java_service.Models.Service;
import com.example.java_service.Repositories.Jpa.BookingRepository;
import com.example.java_service.Repositories.Jpa.BookingServiceRepository;
import com.example.java_service.Repositories.Jpa.ServiceRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingServiceService
{
    BookingServiceRepository bookingServiceRepository;
    ServiceRepository serviceRepository;
    BookingRepository bookingRepository;
    BookingServiceMapper bookingServiceMapper;

    // Hàm đặt dịch vụ
    @Transactional
    public BookingServiceResponse saveBookingService(List<BookingServiceRequest> bookingServiceRequest, UUID idBooking)
    {
        if (bookingServiceRequest == null || bookingServiceRequest.isEmpty()){
            return null;
        }

        Booking booking = bookingRepository.findById(idBooking).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        // Lấy tất cả idService từ request
        List<UUID> idServices = bookingServiceRequest.stream()
                .map(BookingServiceRequest::getIdService)
                .toList();
        List<Service> services = serviceRepository.findAllById(idServices);
        if (services.size() != idServices.size()){
            throw new ApiException(ErrorCode.SERVICE_NOT_FOUND);
        }

        // Xữ lý Race Condition bằng Atomistic
        for (BookingServiceRequest request : bookingServiceRequest)
        {
            UUID idService = request.getIdService();
            int quantity = request.getQuantity();

            int serviceUpdated = serviceRepository.decreaseServiceQuantity(idService, quantity);
            if (serviceUpdated == 0)
                throw new ApiException(ErrorCode.SERVICE_NOT_ENOUGH_QUANTITY);

            serviceRepository.updateStatusIfOutOfService(idService, PredefinedStatusService.STATUS_SERVICE_FULL);
        }

        List<BookingService> bookingServices = bookingServiceRequest.stream()
                .map(request -> {
                    BookingService bookingServiceMapping = bookingServiceMapper.toBookingService(request);
                    bookingServiceMapping.setBooking(booking);

                    // tìm service tương ứng
                    Service service = services.stream()
                            .filter(s -> s.getIdService().equals(request.getIdService()))
                            .findFirst()
                            .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_NOT_FOUND));
                    bookingServiceMapping.setService(service);

                    // giá = đơn giá * số lượng
                    BigDecimal price = service.getServicePrice()
                            .multiply(BigDecimal.valueOf(request.getQuantity()));
                    bookingServiceMapping.setPriceBookingService(price);

                    return bookingServiceMapping;
                }).toList();

        bookingServiceRepository.saveAll(bookingServices);

        // tính tổng giá dịch vụ
        BigDecimal totalPrice = bookingServices.stream()
                .map(BookingService::getPriceBookingService)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BookingServiceResponse bookingServiceResponse = new BookingServiceResponse();
        bookingServiceResponse.setPriceBookingService(totalPrice);
        return bookingServiceResponse;
    }
}
