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
public class RoomBasicResponse
{
    UUID idRoom;
    String typeRoom;
    String roomName;
    BigDecimal roomPrice;
    String size;
    String status;
    Integer quantity;
}