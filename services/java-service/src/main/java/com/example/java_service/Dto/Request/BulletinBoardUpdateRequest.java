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
public class BulletinBoardUpdateRequest {
    UUID idUser;
    String title;
    String content;
    Boolean isFeatured;
    LocalDateTime startDate;
    LocalDateTime endDate;
    String imageUrl;
}