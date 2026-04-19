package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActivityForScheduleResponse {
    UUID idService;
    String serviceName;
    String typeService;
    String description;
    BigDecimal servicePrice;
    String location;
}