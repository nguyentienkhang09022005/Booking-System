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
@Document(collection = "evaluates")
public class Evaluates {
    @Id
    private String id;
    private String refId;
    private String refType; // "accommodation" hoặc "tour"
    private String idUser;
    private Integer stars;
    private String comment;
    private LocalDateTime createdAt;
}