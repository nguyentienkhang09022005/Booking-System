package com.example.java_service.Dto.Response;

import com.example.java_service.Models.Users;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelCreationResponse
{
    UUID idHotel;
    Users users;
    String hotelName;
    String street;
    String ward;
    String city;
    String description;
    List<String> imageHotelUrl;
}
