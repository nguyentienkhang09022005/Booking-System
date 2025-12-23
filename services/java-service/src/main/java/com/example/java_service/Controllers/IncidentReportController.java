package com.example.java_service.Controllers;

import com.example.java_service.Dto.Request.*;
import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.IncidentReportResponse;
import com.example.java_service.Services.IncidentReportService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/incident-report")
public class IncidentReportController
{
    IncidentReportService incidentReportService;

    // Endpoint tạo báo cáo sự cố mới
    @PostMapping("/creation")
    public ApiResponse<IncidentReportResponse> creationIncidentReport(@RequestBody IncidentReportCreationRequest request)
    {
        return ApiResponse.<IncidentReportResponse>builder()
                .message("Tạo báo cáo sự cố thành công!")
                .data(incidentReportService.saveIncidentReport(request))
                .build();
    }

    // Endpoint lấy danh sách báo cáo sự cố của khách sạn
    @GetMapping("/get-list-incident-report-of-hotel")
    public ApiResponse<List<IncidentReportResponse>> getIncidentReports(@RequestParam UUID idHotel)
    {
        return ApiResponse.<List<IncidentReportResponse>>builder()
                .message("Lấy danh sách báo cáo sự cố thành công!")
                .data(incidentReportService.getListIncidentReport(idHotel))
                .build();
    }

    // Endpoint lấy danh sách báo cáo sự cố của khách sạn
    @GetMapping("/get-list-incident-report-of-user")
    public ApiResponse<List<IncidentReportResponse>> getIncidentReportsOfUser(@RequestParam UUID idUser, @RequestParam UUID idBooking)
    {
        return ApiResponse.<List<IncidentReportResponse>>builder()
                .message("Lấy danh sách báo cáo sự cố thành công!")
                .data(incidentReportService.getListIncidentReportByUser(idUser, idBooking))
                .build();
    }

    // Endpoint lấy thông tin chi tiết báo cáo sự cố
    @GetMapping( "/get-inf-incident-report")
    public ApiResponse<IncidentReportResponse> getInfIncidentReport(@RequestParam UUID idIncidentReport)
    {
        return ApiResponse.<IncidentReportResponse>builder()
                .message("Lấy thông tin chi tiết báo cáo sự cố thành công!")
                .data(incidentReportService.getInfIncident(idIncidentReport))
                .build();
    }

    // Endpoint xóa báo cáo sự cố
    @DeleteMapping( "/delele")
    public ApiResponse<ResponseEntity<String>> deleteIncidentReport(@RequestParam UUID idIncidentReport)
    {
        var response = incidentReportService.deleteIncidentReport(idIncidentReport);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    // Endpoint xóa báo cáo sự cố
    @DeleteMapping( "/delele-by-user")
    public ApiResponse<ResponseEntity<String>> deleteIncidentReportByUser(@RequestParam UUID idIncidentReport)
    {
        var response = incidentReportService.deletePendingIncidentReport(idIncidentReport);
        return ApiResponse.<ResponseEntity<String>>builder()
                .message(response.getBody())
                .build();
    }

    // Endpoint cập nhật báo cáo sự cố khi còn pending
    @PatchMapping( "/update-not-approve")
    public ApiResponse<IncidentReportResponse> updateIncidentReport(@RequestParam UUID idIncidentReport,
                                                                    @RequestBody IncidentReportUpdateRequest request)
    {
        return ApiResponse.<IncidentReportResponse>builder()
                .message("Cập nhật thông tin báo cáo sự cố thành công!")
                .data(incidentReportService.updateIncidentReport(request, idIncidentReport))
                .build();
    }

    // Endpoint cập nhật tình trạng báo cáo sự cố
    @PatchMapping( "/approve")
    public ApiResponse<IncidentReportResponse> approveIncidentReport(@RequestParam UUID idIncidentReport,
                                                                     @RequestBody IncidentReportApproveRequest request)
    {
        return ApiResponse.<IncidentReportResponse>builder()
                .message("Cập nhật tình trạng báo cáo sự cố thành công!")
                .data(incidentReportService.approveIncidentReport(request, idIncidentReport))
                .build();
    }
}
