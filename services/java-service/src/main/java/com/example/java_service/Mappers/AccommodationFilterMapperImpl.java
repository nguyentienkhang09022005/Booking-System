package com.example.java_service.Mappers;

import com.example.java_service.Dto.Response.AccommodationFilterResponse;
import com.example.java_service.Models.Accommodation;
import com.example.java_service.Models.Images;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccommodationFilterMapperImpl implements AccommodationFilterMapper {

    @Override
    public AccommodationFilterResponse toResponse(Accommodation accommodation, List<Images> images) {
        List<String> imageUrls = images.stream()
                .map(Images::getUrl)
                .toList();

        return AccommodationFilterResponse.builder()
                .idAccommodation(accommodation.getIdAccommodation())
                .accommodationName(accommodation.getAccommodationName())
                .accommodationType(accommodation.getAccommodationType())
                .street(accommodation.getStreet())
                .ward(accommodation.getWard())
                .city(accommodation.getCity())
                .province(accommodation.getProvince())
                .checkinTime(accommodation.getCheckinTime())
                .checkoutTime(accommodation.getCheckoutTime())
                .description(accommodation.getDescription())
                .imageUrls(imageUrls)
                .build();
    }
}