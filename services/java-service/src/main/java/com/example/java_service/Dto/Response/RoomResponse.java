package com.example.java_service.Dto.Response;

import com.example.java_service.Models.TypeRoom;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomResponse
{
    UUID idRoom;
    TypeRoom typeRoom;
    String roomName;
    BigDecimal roomPrice;
    String size;
    String view;
    String regulations;
    String status;
    String description;
    List<String> imageRoomUrl;
}
