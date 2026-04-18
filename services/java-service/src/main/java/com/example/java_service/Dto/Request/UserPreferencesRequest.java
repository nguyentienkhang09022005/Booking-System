package com.example.java_service.Dto.Request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserPreferencesRequest {
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
}