package com.example.java_service.Models;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "images")
public class Images
{
    @Id
    private String id;
    private String url;
    private String refId; //id tham chiếu (idRoom, idAccommodation, idUser, ...)
    private String type; //"room", "hotel", "user", "advertisement"
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
