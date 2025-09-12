package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelCreationRequest
{
    UUID idUser;
    String hotelName;
    String street;
    String ward;
    String city;
    String description;
    List<String> listImageHotel;
}
