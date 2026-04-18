package com.example.java_service.Dto.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserPreferencesResponse {
    String idPreference;
    String idUser;
    String travelStyle;
    List<String> interests;
    String budgetLevel;
    String dietaryRestrictions;
    String mobilityLimitations;
    String preferredAccommodation;
    List<String> preferredActivities;
    String climatePreference;
    String travelingWith;
    Boolean hasChildren;
    List<Integer> childrenAges;
    Boolean isCompleted;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}