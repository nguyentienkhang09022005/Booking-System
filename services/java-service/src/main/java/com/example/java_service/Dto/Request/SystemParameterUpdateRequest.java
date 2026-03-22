package com.example.java_service.Dto.Request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemParameterUpdateRequest
{
    private String paramValue;
    private String description;
}