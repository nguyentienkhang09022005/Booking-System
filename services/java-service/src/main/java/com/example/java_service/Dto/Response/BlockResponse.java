package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockResponse {
    UUID idBlock;
    UUID idNote;
    String blockType;
    String content;
    Map<String, Object> metadata;
    int displayOrder;
    LocalDateTime createdAt;
    List<String> images;
}