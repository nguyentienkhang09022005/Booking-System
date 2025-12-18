package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationCreatedEvent {
    UUID idNotification;
    UUID idUser;
    String title;
    String content;
    String type;
    String idRef;
    boolean isRead;
    LocalDateTime createdAt;
}

