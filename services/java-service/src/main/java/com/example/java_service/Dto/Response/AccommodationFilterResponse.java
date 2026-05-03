package com.example.java_service.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationFilterResponse {
    private UUID idAccommodation;
    private String accommodationName;
    private String accommodationType;
    private String street;
    private String ward;
    private String city;
    private String province;
    private LocalTime checkinTime;
    private LocalTime checkoutTime;
    private String description;
    private List<String> imageUrls;
}