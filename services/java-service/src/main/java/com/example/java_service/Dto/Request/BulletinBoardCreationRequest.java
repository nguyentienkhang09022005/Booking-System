package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BulletinBoardCreationRequest {
    UUID idUser;
    String title;
    String content;
    String targetAudience;
    Integer priority;
    LocalDateTime startDate;
    LocalDateTime endDate;
    String imageUrl;
}