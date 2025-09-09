package com.example.java_service.Models;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
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
    private String type; //"room", "hotel", "user", "advertisement"
    private String refId; //id tham chiếu (idRoom, idHotel, idUser, ...)
    private LocalDateTime uploadedAt;
}
