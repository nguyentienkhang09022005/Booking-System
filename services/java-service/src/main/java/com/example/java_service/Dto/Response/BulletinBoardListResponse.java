package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BulletinBoardListResponse {
    List<BulletinBoardResponse> leaderboard;      // Tin nổi bật (1 item)
    List<BulletinBoardResponse> featuredGrid;     // Grid 4 cột
    List<BulletinBoardResponse> newestScroll;     // Horizontal scroll
    List<BulletinBoardResponse> expiringSoon;     // Grid + icon cảnh báo
    List<BulletinBoardResponse> regularGrid;     // Grid 3 cột
}