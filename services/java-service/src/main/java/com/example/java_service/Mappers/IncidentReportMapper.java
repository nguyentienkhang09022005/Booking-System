package com.example.java_service.Mappers;

import com.example.java_service.Dto.Request.*;
import com.example.java_service.Dto.Response.IncidentReportResponse;
import com.example.java_service.Models.IncidentReport;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface IncidentReportMapper
{
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "user", ignore = true)
    IncidentReport toIncidentReport(IncidentReportCreationRequest request);

    @Mapping(target = "infUser", ignore = true)
    @Mapping(target = "infBooking", ignore = true)
    IncidentReportResponse toIncidentReportResponse(IncidentReport incidentReport);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateIncidentReportFromRequest(IncidentReportUpdateRequest request, @MappingTarget IncidentReport incidentReport);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void approveIncidentReportFromRequest(IncidentReportApproveRequest request, @MappingTarget IncidentReport incidentReport);
}
