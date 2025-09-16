package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomCreationRequest
{
    UUID idHotel;
    String typeRoom;
    String roomName;
    BigDecimal roomPrice;
    String size;
    String view;
    String regulations;
    String description;
    List<String> listImageRoom;
}
