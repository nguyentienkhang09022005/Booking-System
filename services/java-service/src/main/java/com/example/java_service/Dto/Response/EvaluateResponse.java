package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EvaluateResponse
{
    String id;
    String idAccommodation;
    String idUser;
    Integer stars;
    String comment;
    LocalDateTime createdAt;
}
