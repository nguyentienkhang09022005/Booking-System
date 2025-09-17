package com.example.java_service.Dto.Response;

import com.example.java_service.Models.TypeService;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceResponse
{
    UUID idService;
    TypeService typeService;
    String serviceName;
    BigDecimal servicePrice;
    String status;
    Integer quantity;
    String description;
}
