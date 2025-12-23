package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedSeverityIncident;
import com.example.java_service.Constant.PredefinedStatusIncident;
import com.example.java_service.Dto.Request.*;
import com.example.java_service.Dto.Response.*;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.IncidentReportMapper;
import com.example.java_service.Models.*;
import com.example.java_service.Repositories.Jpa.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IncidentReportService
{
    IncidentReportRepository incidentReportRepository;
    BookingRepository bookingRepository;
    UserRepository userRepository;
    HotelRepository hotelRepository;
    UserRepository hotelUserRepository;
    IncidentReportMapper incidentReportMapper;

    private static final Set<String> VALID_STATUS = Set.of(
            PredefinedStatusIncident.STATUS_INCIDENT_PENDING,
            PredefinedStatusIncident.STATUS_INCIDENT_IN_PROGRESS,
            PredefinedStatusIncident.STATUS_INCIDENT_CANCELLED,
            PredefinedStatusIncident.STATUS_INCIDENT_RESOLVED
    );

    private static final Set<String> VALID_SEVERITIES = Set.of(
            PredefinedSeverityIncident.SEVERITY_INCIDENT_LOW,
            PredefinedSeverityIncident.SEVERITY_INCIDENT_MEDIUM,
            PredefinedSeverityIncident.SEVERITY_INCIDENT_HIGH
    );

    // Hàm lấy danh sách báo cáo sự cố của người dùng
    public List<IncidentReportResponse> getListIncidentReportByUser(UUID idUser)
    {
        userRepository.findById(idUser).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        List<IncidentReport> incidentReportList = incidentReportRepository.findByUser_IdUser(idUser);
        if (incidentReportList.isEmpty()) {
            return List.of();
        }

        return incidentReportList.stream()
                .map(incidentReport -> {
                    IncidentReportResponse incidentReportResponse = incidentReportMapper.toIncidentReportResponse(incidentReport);
                    return incidentReportResponse;
                }).toList();
    }

    // Hàm lấy danh sách báo cáo sự cố của khách sạn
    public List<IncidentReportResponse> getListIncidentReport(UUID idHotel)
    {
        hotelRepository.findById(idHotel).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );

        List<IncidentReport> incidentReportList = incidentReportRepository.findByBooking_Hotel_IdHotel(idHotel);
        if (incidentReportList.isEmpty()) {
            return List.of();
        }

        return incidentReportList.stream()
                .map(incidentReport -> {
                    IncidentReportResponse incidentReportResponse = incidentReportMapper.toIncidentReportResponse(incidentReport);
                    return incidentReportResponse;
                }).toList();
    }

    // Hàm lấy thông tin báo cáo sự cố
    public IncidentReportResponse getInfIncident(UUID idIncidentReport)
    {
        IncidentReport incidentReport = incidentReportRepository.findById(idIncidentReport).orElseThrow(
                () -> new ApiException(ErrorCode.INCIDENT_REPORT_NOT_FOUND)
        );

        Users user = userRepository.findById(incidentReport.getUser().getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Booking booking = bookingRepository.findById(incidentReport.getBooking().getIdBooking()).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        IncidentReportResponse incidentReportResponse = incidentReportMapper.toIncidentReportResponse(incidentReport);

        InfUserCreatedIncidentReportResponse infUserCreatedIncidentReportResponse = InfUserCreatedIncidentReportResponse.builder()
                .idUser(user.getIdUser())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dob(user.getDob())
                .address(user.getAddress())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .build();

        InfBookingOfIncidentReportResponse infBookingOfIncidentReportResponse = InfBookingOfIncidentReportResponse.builder()
                .idBooking(booking.getIdBooking())
                .bookingDate(booking.getBookingDate())
                .checkinDate(booking.getCheckinDate())
                .checkoutDate(booking.getCheckoutDate())
                .status(booking.getStatus())
                .totalPrice(booking.getTotalPrice())
                .build();

        incidentReportResponse.setInfUser(infUserCreatedIncidentReportResponse);
        incidentReportResponse.setInfBooking(infBookingOfIncidentReportResponse);
        return incidentReportResponse;
    }

    // Hàm thêm báo cáo sự cố mới
    public IncidentReportResponse saveIncidentReport(IncidentReportCreationRequest request)
    {
        Users user = userRepository.findById(request.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Booking booking = bookingRepository.findById(request.getIdBooking()).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        IncidentReport incidentReport = incidentReportMapper.toIncidentReport(request);
        incidentReport.setBooking(booking);
        incidentReport.setUser(user);
        incidentReport.setStatus(PredefinedStatusIncident.STATUS_INCIDENT_PENDING);

        IncidentReport savedIncidentReport = incidentReportRepository.save(incidentReport);

        IncidentReportResponse incidentReportResponse = incidentReportMapper.toIncidentReportResponse(savedIncidentReport);

        InfUserCreatedIncidentReportResponse infUserCreatedIncidentReportResponse = InfUserCreatedIncidentReportResponse.builder()
                .idUser(user.getIdUser())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dob(user.getDob())
                .address(user.getAddress())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .build();

        InfBookingOfIncidentReportResponse infBookingOfIncidentReportResponse = InfBookingOfIncidentReportResponse.builder()
                .idBooking(booking.getIdBooking())
                .bookingDate(booking.getBookingDate())
                .checkinDate(booking.getCheckinDate())
                .checkoutDate(booking.getCheckoutDate())
                .status(booking.getStatus())
                .totalPrice(booking.getTotalPrice())
                .build();

        incidentReportResponse.setInfUser(infUserCreatedIncidentReportResponse);
        incidentReportResponse.setInfBooking(infBookingOfIncidentReportResponse);
        return incidentReportResponse;
    }

    //  Hàm xóa báo cáo sự cố
    public ResponseEntity<String> deleteIncidentReport(UUID idIncidentReport)
    {
        IncidentReport incidentReport = incidentReportRepository.findById(idIncidentReport).orElseThrow(
                () -> new ApiException(ErrorCode.INCIDENT_REPORT_NOT_FOUND)
        );

        incidentReportRepository.delete(incidentReport);
        return ResponseEntity.ok("Xóa báo cáo sự cố thành công!");
    }

    // Hàm xóa báo cáo sự cố dành cho User (chỉ xóa được khi còn pending và đúng chủ sở hữu)
    public ResponseEntity<String> deletePendingIncidentReport(UUID idIncidentReport, UUID idUser)
    {
        IncidentReport incidentReport = incidentReportRepository.findById(idIncidentReport).orElseThrow(
                () -> new ApiException(ErrorCode.INCIDENT_REPORT_NOT_FOUND)
        );

        if (!incidentReport.getUser().getIdUser().equals(idUser)) {
            throw new ApiException(ErrorCode.INCIDENT_REPORT_NOT_FOUND);
        }

        if (!incidentReport.getStatus().equals(PredefinedStatusIncident.STATUS_INCIDENT_PENDING)) {
            throw new ApiException(ErrorCode.INVALID_INCIDENT_REPORT);
        }

        incidentReportRepository.delete(incidentReport);

        return ResponseEntity.ok("Xóa báo cáo sự cố thành công!");
    }

    // Hàm cập nhật tình trạng báo cáo sự cố
    public IncidentReportResponse approveIncidentReport(IncidentReportApproveRequest request, UUID idIncidentReport)
    {
        IncidentReport incidentReport = incidentReportRepository.findById(idIncidentReport).orElseThrow(
                () -> new ApiException(ErrorCode.INCIDENT_REPORT_NOT_FOUND)
        );

        if (!VALID_STATUS.contains(request.getStatus())) {
            throw new ApiException(ErrorCode.INVALID_STATUS_INCIDENT_REPORT);
        }

        if (!VALID_SEVERITIES.contains(request.getSeverity())) {
            throw new ApiException(ErrorCode.INVALID_SEVERITY_INCIDENT_REPORT);
        }

        Users user = userRepository.findById(incidentReport.getUser().getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Booking booking = bookingRepository.findById(incidentReport.getBooking().getIdBooking()).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        incidentReportMapper.approveIncidentReportFromRequest(request, incidentReport);

        IncidentReport approvedIncidentReport = incidentReportRepository.save(incidentReport);

        IncidentReportResponse incidentReportResponse = incidentReportMapper.toIncidentReportResponse(approvedIncidentReport);

        InfUserCreatedIncidentReportResponse infUserCreatedIncidentReportResponse = InfUserCreatedIncidentReportResponse.builder()
                .idUser(incidentReport.getUser().getIdUser())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dob(user.getDob())
                .address(user.getAddress())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .build();

        InfBookingOfIncidentReportResponse infBookingOfIncidentReportResponse = InfBookingOfIncidentReportResponse.builder()
                .idBooking(booking.getIdBooking())
                .bookingDate(booking.getBookingDate())
                .checkinDate(booking.getCheckinDate())
                .checkoutDate(booking.getCheckoutDate())
                .status(booking.getStatus())
                .totalPrice(booking.getTotalPrice())
                .build();

        incidentReportResponse.setInfUser(infUserCreatedIncidentReportResponse);
        incidentReportResponse.setInfBooking(infBookingOfIncidentReportResponse);
        return incidentReportResponse;
    }

    // Hàm thay đổi thông tin báo cáo sự cố khi còn pending
    public IncidentReportResponse updateIncidentReport(IncidentReportUpdateRequest request, UUID idIncidentReport)
    {
        IncidentReport incidentReport = incidentReportRepository.findById(idIncidentReport).orElseThrow(
                () -> new ApiException(ErrorCode.INCIDENT_REPORT_NOT_FOUND)
        );

        if (!incidentReport.getStatus().equals(PredefinedStatusIncident.STATUS_INCIDENT_PENDING)){
            throw new ApiException(ErrorCode.INVALID_INCIDENT_REPORT);
        }

        Users user = userRepository.findById(incidentReport.getUser().getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Booking booking = bookingRepository.findById(incidentReport.getBooking().getIdBooking()).orElseThrow(
                () -> new ApiException(ErrorCode.BOOKING_NOT_FOUND)
        );

        incidentReportMapper.updateIncidentReportFromRequest(request, incidentReport);

        IncidentReport approvedIncidentReport = incidentReportRepository.save(incidentReport);

        IncidentReportResponse incidentReportResponse = incidentReportMapper.toIncidentReportResponse(approvedIncidentReport);

        InfUserCreatedIncidentReportResponse infUserCreatedIncidentReportResponse = InfUserCreatedIncidentReportResponse.builder()
                .idUser(incidentReport.getUser().getIdUser())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dob(user.getDob())
                .address(user.getAddress())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .build();

        InfBookingOfIncidentReportResponse infBookingOfIncidentReportResponse = InfBookingOfIncidentReportResponse.builder()
                .idBooking(booking.getIdBooking())
                .bookingDate(booking.getBookingDate())
                .checkinDate(booking.getCheckinDate())
                .checkoutDate(booking.getCheckoutDate())
                .status(booking.getStatus())
                .totalPrice(booking.getTotalPrice())
                .build();

        incidentReportResponse.setInfUser(infUserCreatedIncidentReportResponse);
        incidentReportResponse.setInfBooking(infBookingOfIncidentReportResponse);
        return incidentReportResponse;
    }
}
