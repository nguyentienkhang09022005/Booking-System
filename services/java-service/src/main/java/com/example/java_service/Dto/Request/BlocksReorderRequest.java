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
public class BlocksReorderRequest {
    UUID idUser;
    List<BlockOrder> blocks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockOrder {
        UUID idBlock;
        int displayOrder;
    }
}