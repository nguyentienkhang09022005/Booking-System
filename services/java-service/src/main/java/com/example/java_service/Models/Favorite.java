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
@Document(collection = "favorites")
public class Favorite
{
    @Id
    private String id;
    private String idUser;
    private String idAccommodation;
    private LocalDateTime createdAt;
}
