package com.example.java_service.Dto.Response;

import com.example.java_service.Models.Users;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BulletinBoardResponse {
    UUID idBulletin;
    String title;
    String content;
    String targetAudience;
    String status;
    Integer priority;
    Boolean isApproved;
    LocalDateTime startDate;
    LocalDateTime endDate;
    LocalDateTime createdAt;
    LocalDateTime approvedAt;

    // Author info
    AuthorInfo author;

    // Images (from MongoDB images collection)
    java.util.List<String> images;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthorInfo {
        UUID idUser;
        String firstName;
        String lastName;
        String email;
    }
}