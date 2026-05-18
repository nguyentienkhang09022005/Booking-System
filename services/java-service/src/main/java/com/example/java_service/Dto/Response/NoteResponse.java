package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteResponse {
    UUID idNote;
    UUID idNotebook;
    String title;
    int displayOrder;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<BlockResponse> blocks;
}