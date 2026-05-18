package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotesReorderRequest {
    UUID idUser;
    List<NoteOrder> notes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoteOrder {
        UUID idNote;
        int displayOrder;
    }
}