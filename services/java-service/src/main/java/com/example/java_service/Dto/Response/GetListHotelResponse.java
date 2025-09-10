package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GetListHotelResponse
{
    UUID idHotel;
    String hotelName;
    String street;
    String ward;
    String city;
    Boolean favorite;
    String imageHotelUrl;
}
