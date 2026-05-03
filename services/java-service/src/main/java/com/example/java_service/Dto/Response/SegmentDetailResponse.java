package com.example.java_service.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentDetailResponse {
    private String segment;
    private SegmentDistributionResponse.SegmentStatsDTO data;
}