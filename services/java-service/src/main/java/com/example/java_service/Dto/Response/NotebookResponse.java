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
public class NotebookResponse {
    UUID idNotebook;
    String title;
    String description;
    String coverImage;
    String template;
    Boolean isFavorite;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    OwnerInfo owner;
    List<NoteDetail> notes;
    List<String> images;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerInfo {
        UUID idUser;
        String firstName;
        String lastName;
        String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoteDetail {
        UUID idNote;
        String title;
        int displayOrder;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;
        List<BlockDetail> blocks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockDetail {
        UUID idBlock;
        UUID idNote;
        String blockType;
        String content;
        Map<String, Object> metadata;
        int displayOrder;
        LocalDateTime createdAt;
        List<String> images;
    }
}