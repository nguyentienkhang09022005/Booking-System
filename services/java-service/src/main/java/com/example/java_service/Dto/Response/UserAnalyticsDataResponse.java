package com.example.java_service.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnalyticsDataResponse {
    private String userId;
    private String firstName;
    private String lastName;
    private Integer loyaltyPoints;
    private String currentSegment;
    private PreferencesDTO preferences;
    private Integer totalBookings;
    private Long totalSpent;
    private Integer avgBookingValue;
    private Integer totalNights;
    private Integer totalTrips;
    private Map<String, Integer> recentBookingMonths;
    private List<BookingSummaryDTO> recentBookings;

    private Integer accommodationCount;
    private Long accommodationTotalSpent;
    private Integer tourCount;
    private Long tourTotalSpent;
    private Integer transportCount;
    private Long transportTotalSpent;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreferencesDTO {
        private String travelStyle;
        private String budgetLevel;
        private String travelingWith;
        private Boolean hasChildren;
        private List<String> interests;
        private List<String> preferredActivities;
        private String preferredAccommodation;
        private Boolean isCompleted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingSummaryDTO {
        private String bookingType;
        private String status;
        private Double totalPrice;
        private String checkinDate;
        private String checkoutDate;
        private String accommodationType;
    }
}