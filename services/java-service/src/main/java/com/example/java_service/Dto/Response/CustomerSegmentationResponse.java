package com.example.java_service.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSegmentationResponse {
    private String segment;
    private Double confidence;
    private String reasoning;
}