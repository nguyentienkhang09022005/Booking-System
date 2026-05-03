package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedBudgetLevel;
import com.example.java_service.Constant.PredefinedTravelStyle;
import com.example.java_service.Constant.PredefinedTravelingWith;
import com.example.java_service.Dto.Request.UserPreferencesRequest;
import com.example.java_service.Dto.Response.UserPreferencesResponse;
import com.example.java_service.Events.BookingCompletedEvent;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.UserPreferencesMapper;
import com.example.java_service.Models.UserPreferences;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.UserPreferencesRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserPreferencesService {

    UserPreferencesRepository userPreferencesRepository;
    UserRepository userRepository;
    UserPreferencesMapper userPreferencesMapper;
    ApplicationEventPublisher eventPublisher;

    private static final Set<String> VALID_TRAVEL_STYLES = new HashSet<>(Arrays.asList(
            PredefinedTravelStyle.ADVENTURE,
            PredefinedTravelStyle.RELAXATION,
            PredefinedTravelStyle.CULTURAL,
            PredefinedTravelStyle.FOODIE,
            PredefinedTravelStyle.SHOPPING,
            PredefinedTravelStyle.NATURE
    ));

    private static final Set<String> VALID_BUDGET_LEVELS = new HashSet<>(Arrays.asList(
            PredefinedBudgetLevel.BUDGET,
            PredefinedBudgetLevel.MODERATE,
            PredefinedBudgetLevel.LUXURY
    ));

    private static final Set<String> VALID_TRAVELING_WITH = new HashSet<>(Arrays.asList(
            PredefinedTravelingWith.SOLO,
            PredefinedTravelingWith.COUPLE,
            PredefinedTravelingWith.FAMILY,
            PredefinedTravelingWith.FRIENDS,
            PredefinedTravelingWith.GROUP
    ));

    public UserPreferencesResponse getPreferences(String idUser) {
        UUID userId = UUID.fromString(idUser);
        UserPreferences preferences = userPreferencesRepository.findByUsers_IdUser(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_PREFERENCES_NOT_FOUND));
        return userPreferencesMapper.toResponse(preferences);
    }

    public UserPreferencesResponse getPreferencesOrDefault(String idUser) {
        UUID userId = UUID.fromString(idUser);
        return userPreferencesRepository.findByUsers_IdUser(userId)
                .map(userPreferencesMapper::toResponse)
                .orElse(getDefaultPreferencesResponse(idUser));
    }

    public boolean hasCompletedOnboarding(String idUser) {
        UUID userId = UUID.fromString(idUser);
        return userPreferencesRepository.findByUsers_IdUser(userId)
                .map(UserPreferences::getIsCompleted)
                .orElse(false);
    }

    @Transactional
    public UserPreferencesResponse createOrUpdate(String idUser, UserPreferencesRequest request) {
        UUID userUuid = UUID.fromString(idUser);
        Users user = userRepository.findById(userUuid)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        UserPreferences preferences = userPreferencesRepository.findByUsers_IdUser(userUuid)
                .orElse(null);

        if (preferences == null) {
            preferences = userPreferencesMapper.toUserPreferences(request);
            preferences.setUsers(user);
        } else {
            userPreferencesMapper.updateFromRequest(request, preferences);
        }

        validateAndNormalizePreferences(preferences);

        UserPreferences saved = userPreferencesRepository.save(preferences);

        eventPublisher.publishEvent(BookingCompletedEvent.builder()
                .userId(userUuid)
                .build());

        return userPreferencesMapper.toResponse(saved);
    }

    public UserPreferences getPreferencesEntityOrDefault(String idUser) {
        UUID userId = UUID.fromString(idUser);
        UserPreferences preferences = userPreferencesRepository.findByUsers_IdUser(userId)
                .orElse(null);

        if (preferences != null) {
            validateAndNormalizePreferences(preferences);
        }

        return preferences;
    }

    public UserPreferencesResponse getDefaultPreferencesResponse(String idUser) {
        return UserPreferencesResponse.builder()
                .idUser(idUser)
                .travelStyle(PredefinedTravelStyle.DEFAULT_TRAVEL_STYLE)
                .budgetLevel(PredefinedBudgetLevel.DEFAULT_BUDGET_LEVEL)
                .travelingWith(PredefinedTravelingWith.DEFAULT_TRAVELING_WITH)
                .hasChildren(false)
                .interests(new ArrayList<>())
                .preferredActivities(new ArrayList<>())
                .childrenAges(new ArrayList<>())
                .isCompleted(false)
                .build();
    }

    public void validateAndNormalizePreferences(UserPreferences preferences) {
        if (preferences == null) {
            return;
        }

        if (preferences.getTravelStyle() != null) {
            String style = preferences.getTravelStyle().toLowerCase().trim();
            if (!VALID_TRAVEL_STYLES.contains(style)) {
                log.warn("Invalid travelStyle: '{}', using default: {}", style, PredefinedTravelStyle.DEFAULT_TRAVEL_STYLE);
                preferences.setTravelStyle(PredefinedTravelStyle.DEFAULT_TRAVEL_STYLE);
            } else {
                preferences.setTravelStyle(style);
            }
        } else {
            preferences.setTravelStyle(PredefinedTravelStyle.DEFAULT_TRAVEL_STYLE);
        }

        if (preferences.getBudgetLevel() != null) {
            String level = preferences.getBudgetLevel().toLowerCase().trim();
            if (!VALID_BUDGET_LEVELS.contains(level)) {
                log.warn("Invalid budgetLevel: '{}', using default: {}", level, PredefinedBudgetLevel.DEFAULT_BUDGET_LEVEL);
                preferences.setBudgetLevel(PredefinedBudgetLevel.DEFAULT_BUDGET_LEVEL);
            } else {
                preferences.setBudgetLevel(level);
            }
        } else {
            preferences.setBudgetLevel(PredefinedBudgetLevel.DEFAULT_BUDGET_LEVEL);
        }

        if (preferences.getTravelingWith() != null) {
            String travelingWith = preferences.getTravelingWith().toLowerCase().trim();
            if (!VALID_TRAVELING_WITH.contains(travelingWith)) {
                log.warn("Invalid travelingWith: '{}', using default: {}", travelingWith, PredefinedTravelingWith.DEFAULT_TRAVELING_WITH);
                preferences.setTravelingWith(PredefinedTravelingWith.DEFAULT_TRAVELING_WITH);
            } else {
                preferences.setTravelingWith(travelingWith);
            }
        } else {
            preferences.setTravelingWith(PredefinedTravelingWith.DEFAULT_TRAVELING_WITH);
        }
    }
}
