package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomUpdateRequest
{
    String typeRoom;
    String roomName;
    BigDecimal roomPrice;
    String size;
    String view;
    String regulations;
    String status;
    String description;
    Integer quantity;
    List<String> listImageRoom;
}
