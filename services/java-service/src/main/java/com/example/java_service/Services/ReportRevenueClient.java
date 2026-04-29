package com.example.java_service.Services;

import com.example.java_service.Dto.Response.ApiResponse;
import com.example.java_service.Dto.Response.RevenueCreatedEvent;
import com.example.java_service.Dto.Response.RevenueReportResponse;
import com.example.java_service.Dto.Response.RevenueUpdateEvent;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportRevenueClient
{
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ReportRevenueClient(WebClient webClient, ObjectMapper objectMapper)
    {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public RevenueReportResponse createRevenueDetail(RevenueCreatedEvent event)
    {
        try
        {
            ApiResponse<?> response = webClient.post()
                    .uri("/report-revenues-detail/create")
                    .header("Content-Type", "application/json")
                    .bodyValue(event)
                    .retrieve()
                    .bodyToMono(ApiResponse.class)
                    .block();

            if (response == null)
            {
                throw new ApiException(ErrorCode.REVENUE_REPORT_FAILED);
            }

            if (!"200".equals(response.getCode()) && !"201".equals(response.getCode()))
            {
                throw new ApiException(ErrorCode.REVENUE_REPORT_FAILED);
            }

            RevenueReportResponse revenueResponse = objectMapper.convertValue(response.getData(), RevenueReportResponse.class);
            return revenueResponse;
        }
        catch (ApiException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("Lỗi khi gọi API tạo chi tiết báo cáo doanh thu: {}", e.getMessage(), e);
            throw new ApiException(ErrorCode.REVENUE_REPORT_FAILED);
        }
    }

    public void updateRevenueDetail(RevenueUpdateEvent event)
    {
        try
        {
            ApiResponse<?> response = webClient.put()
                    .uri("/report-revenues-detail/update")
                    .header("Content-Type", "application/json")
                    .bodyValue(event)
                    .retrieve()
                    .bodyToMono(ApiResponse.class)
                    .block();

            if (response == null)
            {
                throw new ApiException(ErrorCode.REVENUE_REPORT_FAILED);
            }

            if (!"200".equals(response.getCode()) && !"204".equals(response.getCode()))
            {
                throw new ApiException(ErrorCode.REVENUE_REPORT_FAILED);
            }
        }
        catch (ApiException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("Lỗi khi gọi API cập nhật chi tiết báo cáo doanh thu: {}", e.getMessage(), e);
            throw new ApiException(ErrorCode.REVENUE_REPORT_FAILED);
        }
    }
}
