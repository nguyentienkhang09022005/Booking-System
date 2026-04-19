package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TourInclusionsExclusionsResponse
{
    List<String> inclusions;
    List<String> exclusions;
    List<String> additionalInfo;
}