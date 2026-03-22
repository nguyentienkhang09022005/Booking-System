package com.example.java_service.Dto.Response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemParameterResponse
{
    private String paramKey;
    private String paramValue;
    private String description;
    private LocalDateTime updatedAt;
}