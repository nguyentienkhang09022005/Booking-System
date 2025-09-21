// HotelStatsResponse.java
package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelStatsResponse {
    String idHotel;
    Long allEvaluates;
    Double averageStar;
}