package com.example.java_service.Helpers;

import com.example.java_service.Dto.Response.UserPreferencesResponse;
import com.example.java_service.Repositories.Jpa.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class SegmentationTools {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final ObjectMapper objectMapper;

    @Tool("Lấy thông tin user: idUser (UUID)")
    public String getUserInfo(String idUser) {
        try {
            return userRepository.findById(UUID.fromString(idUser))
                    .map(user -> {
                        try {
                            return objectMapper.writeValueAsString(new UserInfo(
                                user.getFirstName(),
                                user.getLastName(),
                                user.getEmail(),
                                user.getPhone(),
                                user.getCustomerSegment(),
                                user.getSegmentConfidence(),
                                user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0
                            ));
                        } catch (Exception e) {
                            return "Error serializing user info";
                        }
                    })
                    .orElse("User not found");
        } catch (Exception e) {
            log.error("Tool Error - getUserInfo: {}", e.getMessage());
            return "Error getting user info";
        }
    }

    @Tool("Lấy preferences của user: idUser")
    public String getUserPreferences(String idUser) {
        try {
            return userPreferencesRepository.findByUsers_IdUser(UUID.fromString(idUser))
                    .map(pref -> {
                        try {
                            return objectMapper.writeValueAsString(new UserPreferencesInfo(
                                pref.getTravelStyle(),
                                pref.getBudgetLevel(),
                                pref.getTravelingWith(),
                                pref.getHasChildren() != null && pref.getHasChildren(),
                                pref.getInterests(),
                                pref.getPreferredActivities(),
                                pref.getPreferredAccommodation(),
                                pref.getIsCompleted() != null && pref.getIsCompleted()
                            ));
                        } catch (Exception e) {
                            return "Error serializing preferences";
                        }
                    })
                    .orElse("{\"travelStyle\":\"default\",\"budgetLevel\":\"moderate\",\"travelingWith\":\"solo\",\"hasChildren\":false,\"interests\":[],\"preferredActivities\":[],\"preferredAccommodation\":\"hotel\",\"isCompleted\":false}");
        } catch (Exception e) {
            log.error("Tool Error - getUserPreferences: {}", e.getMessage());
            return "{}";
        }
    }

    @Tool("Lấy số lượng booking đã hoàn thành của user: idUser")
    public String getUserBookingCount(String idUser) {
        try {
            int count = bookingRepository.findByUsers_IdUser(UUID.fromString(idUser))
                    .stream()
                    .filter(b -> "Success".equals(b.getStatus()) || "Checked Out".equals(b.getStatus()))
                    .mapToInt(b -> 1)
                    .sum();
            return String.valueOf(count);
        } catch (Exception e) {
            log.error("Tool Error - getUserBookingCount: {}", e.getMessage());
            return "0";
        }
    }

    @Tool("Lấy danh sách loại accommodation đã booking: idUser")
    public String getUserAccommodationTypes(String idUser) {
        try {
            var bookings = bookingRepository.findByUsers_IdUser(UUID.fromString(idUser));
            long accommodationCount = bookings.stream()
                    .filter(b -> b.getAccommodation() != null)
                    .map(b -> b.getAccommodation().getIdAccommodation())
                    .distinct()
                    .count();
            return String.valueOf(accommodationCount);
        } catch (Exception e) {
            log.error("Tool Error - getUserAccommodationTypes: {}", e.getMessage());
            return "0";
        }
    }

    @Tool("Lấy tổng chi tiêu của user: idUser")
    public String getUserTotalSpent(String idUser) {
        try {
            var bookings = bookingRepository.findByUsers_IdUser(UUID.fromString(idUser));
            double total = bookings.stream()
                    .filter(b -> ("Success".equals(b.getStatus()) || "Checked Out".equals(b.getStatus())))
                    .mapToDouble(b -> b.getTotalPrice() != null ? b.getTotalPrice().doubleValue() : 0)
                    .sum();
            return String.valueOf(total);
        } catch (Exception e) {
            log.error("Tool Error - getUserTotalSpent: {}", e.getMessage());
            return "0";
        }
    }

    public record UserInfo(
        String firstName,
        String lastName,
        String email,
        String phone,
        String currentSegment,
        Double segmentConfidence,
        int loyaltyPoints
    ) {}

    public record UserPreferencesInfo(
        String travelStyle,
        String budgetLevel,
        String travelingWith,
        boolean hasChildren,
        java.util.List<String> interests,
        java.util.List<String> preferredActivities,
        String preferredAccommodation,
        boolean isCompleted
    ) {}
}