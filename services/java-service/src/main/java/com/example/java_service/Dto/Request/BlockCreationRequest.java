package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockCreationRequest {
    UUID idUser;
    String blockType;
    String content;
    Map<String, Object> metadata;
    Integer displayOrder;
}