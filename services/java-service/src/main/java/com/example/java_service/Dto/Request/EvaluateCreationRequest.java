package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EvaluateCreationRequest
{
    String idAccommodation;
    String idUser;
    Integer stars;
    String comment;
}
