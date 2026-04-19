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
public class ServiceDetailResponse
{
    UUID idService;
    String typeService;
    String serviceName;
    BigDecimal servicePrice;
    String status;
    Integer quantity;
    String description;

    String accommodationName;
    String accommodationStreet;
    String accommodationWard;
    String accommodationCity;
    String accommodationProvince;
}