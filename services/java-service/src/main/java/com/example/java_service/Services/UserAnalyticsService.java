package com.example.java_service.Services;

import com.example.java_service.Dto.Response.UserAnalyticsDataResponse;
import com.example.java_service.Models.Booking;
import com.example.java_service.Models.BookingTour;
import com.example.java_service.Models.BookingTransport;
import com.example.java_service.Models.UserPreferences;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.BookingRepository;
import com.example.java_service.Repositories.Jpa.BookingTourRepository;
import com.example.java_service.Repositories.Jpa.BookingTransportTicketRepository;
import com.example.java_service.Repositories.Jpa.UserPreferencesRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAnalyticsService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final BookingTourRepository bookingTourRepository;
    private final BookingTransportTicketRepository bookingTransportTicketRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final ObjectMapper objectMapper;

    public String getUserAnalyticsData(UUID userId) {
        try {
            UserAnalyticsDataResponse response = buildUserAnalyticsData(userId);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Error getting analytics data for user {}: {}", userId, e.getMessage());
            return "{}";
        }
    }

    public UserAnalyticsDataResponse buildUserAnalyticsData(UUID userId) {
        Users user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return UserAnalyticsDataResponse.builder().build();
        }

        UserPreferences preferences = userPreferencesRepository.findByUsers_IdUser(userId).orElse(null);

        List<Booking> accommodationBookings = bookingRepository.findByUsers_IdUser(userId);
        List<BookingTour> tourBookings = bookingTourRepository.findByUsers_IdUser(userId);
        List<BookingTransport> transportBookings = bookingTransportTicketRepository.findByUsers_IdUser(userId);

        List<UserAnalyticsDataResponse.BookingSummaryDTO> allBookingsSummary = new ArrayList<>();
        int completedBookings = 0;
        double totalSpent = 0;
        int totalNights = 0;
        int totalTrips = 0;
        Map<String, Integer> bookingMonths = new HashMap<>();

        int accommodationCount = 0;
        double accommodationTotalSpent = 0;
        int tourCount = 0;
        double tourTotalSpent = 0;
        int transportCount = 0;
        double transportTotalSpent = 0;

        for (Booking booking : accommodationBookings) {
            String status = booking.getStatus() != null ? booking.getStatus() : "Unknown";
            double totalPrice = booking.getTotalPrice() != null ? booking.getTotalPrice().doubleValue() : 0;
            String accommodationType = booking.getAccommodation() != null ? "hotel" : "none";

            allBookingsSummary.add(UserAnalyticsDataResponse.BookingSummaryDTO.builder()
                    .bookingType("ACCOMMODATION")
                    .status(status)
                    .totalPrice(totalPrice)
                    .checkinDate(booking.getCheckinDate() != null ? booking.getCheckinDate().toString() : "")
                    .checkoutDate(booking.getCheckoutDate() != null ? booking.getCheckoutDate().toString() : "")
                    .accommodationType(accommodationType)
                    .build());

            if ("Success".equals(status) || "Checked Out".equals(status)) {
                completedBookings++;
                totalSpent += totalPrice;
                accommodationCount++;
                accommodationTotalSpent += totalPrice;
                totalTrips++;

                if (booking.getCheckinDate() != null && booking.getCheckoutDate() != null) {
                    long nights = java.time.temporal.ChronoUnit.DAYS.between(
                        booking.getCheckinDate().toLocalDate(),
                        booking.getCheckoutDate().toLocalDate()
                    );
                    totalNights += nights;
                }

                if (booking.getBookingDate() != null) {
                    String monthKey = booking.getBookingDate().getYear() + "-" + 
                        String.format("%02d", booking.getBookingDate().getMonthValue());
                    bookingMonths.put(monthKey, bookingMonths.getOrDefault(monthKey, 0) + 1);
                }
            }
        }

        for (BookingTour bookingTour : tourBookings) {
            String status = bookingTour.getStatus() != null ? bookingTour.getStatus() : "Unknown";
            double totalPrice = bookingTour.getPriceBookingTour() != null ? bookingTour.getPriceBookingTour().doubleValue() : 0;

            allBookingsSummary.add(UserAnalyticsDataResponse.BookingSummaryDTO.builder()
                    .bookingType("TOUR")
                    .status(status)
                    .totalPrice(totalPrice)
                    .checkinDate(bookingTour.getCreatedAt() != null ? bookingTour.getCreatedAt().toString() : "")
                    .checkoutDate("")
                    .accommodationType(bookingTour.getTour() != null ? "tour" : "none")
                    .build());

            if ("Success".equals(status)) {
                completedBookings++;
                totalSpent += totalPrice;
                tourCount++;
                tourTotalSpent += totalPrice;
                totalTrips++;

                if (bookingTour.getCreatedAt() != null) {
                    String monthKey = bookingTour.getCreatedAt().getYear() + "-" + 
                        String.format("%02d", bookingTour.getCreatedAt().getMonthValue());
                    bookingMonths.put(monthKey, bookingMonths.getOrDefault(monthKey, 0) + 1);
                }
            }
        }

        for (BookingTransport bookingTransport : transportBookings) {
            String status = bookingTransport.getStatus() != null ? bookingTransport.getStatus() : "Unknown";
            double totalPrice = bookingTransport.getPriceBookingTransport() != null ? bookingTransport.getPriceBookingTransport().doubleValue() : 0;

            allBookingsSummary.add(UserAnalyticsDataResponse.BookingSummaryDTO.builder()
                    .bookingType("TRANSPORT")
                    .status(status)
                    .totalPrice(totalPrice)
                    .checkinDate(bookingTransport.getCreatedAt() != null ? bookingTransport.getCreatedAt().toString() : "")
                    .checkoutDate("")
                    .accommodationType(bookingTransport.getTransportTicket() != null ? "transport" : "none")
                    .build());

            if ("Success".equals(status)) {
                completedBookings++;
                totalSpent += totalPrice;
                transportCount++;
                transportTotalSpent += totalPrice;

                if (bookingTransport.getCreatedAt() != null) {
                    String monthKey = bookingTransport.getCreatedAt().getYear() + "-" + 
                        String.format("%02d", bookingTransport.getCreatedAt().getMonthValue());
                    bookingMonths.put(monthKey, bookingMonths.getOrDefault(monthKey, 0) + 1);
                }
            }
        }

        UserAnalyticsDataResponse.PreferencesDTO preferencesDTO = null;
        if (preferences != null) {
            preferencesDTO = UserAnalyticsDataResponse.PreferencesDTO.builder()
                    .travelStyle(preferences.getTravelStyle() != null ? preferences.getTravelStyle() : "default")
                    .budgetLevel(preferences.getBudgetLevel() != null ? preferences.getBudgetLevel() : "moderate")
                    .travelingWith(preferences.getTravelingWith() != null ? preferences.getTravelingWith() : "solo")
                    .hasChildren(preferences.getHasChildren() != null && preferences.getHasChildren())
                    .interests(preferences.getInterests() != null ? preferences.getInterests() : new ArrayList<>())
                    .preferredActivities(preferences.getPreferredActivities() != null ? preferences.getPreferredActivities() : new ArrayList<>())
                    .preferredAccommodation(preferences.getPreferredAccommodation() != null ? preferences.getPreferredAccommodation() : "hotel")
                    .isCompleted(preferences.getIsCompleted() != null && preferences.getIsCompleted())
                    .build();
        } else {
            preferencesDTO = UserAnalyticsDataResponse.PreferencesDTO.builder()
                    .travelStyle("default")
                    .budgetLevel("moderate")
                    .travelingWith("solo")
                    .hasChildren(false)
                    .interests(new ArrayList<>())
                    .preferredActivities(new ArrayList<>())
                    .preferredAccommodation("hotel")
                    .isCompleted(false)
                    .build();
        }

        List<UserAnalyticsDataResponse.BookingSummaryDTO> sortedBookings = allBookingsSummary.stream()
                .sorted(Comparator.comparing(UserAnalyticsDataResponse.BookingSummaryDTO::getCheckinDate, 
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(15)
                .collect(Collectors.toList());

        return UserAnalyticsDataResponse.builder()
                .userId(userId.toString())
                .firstName(user.getFirstName() != null ? user.getFirstName() : "")
                .lastName(user.getLastName() != null ? user.getLastName() : "")
                .loyaltyPoints(user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0)
                .currentSegment(user.getCustomerSegment() != null ? user.getCustomerSegment() : "UNKNOWN")
                .preferences(preferencesDTO)
                .totalBookings(completedBookings)
                .totalSpent(Math.round(totalSpent))
                .avgBookingValue(completedBookings > 0 ? (int) Math.round(totalSpent / completedBookings) : 0)
                .totalNights(totalNights)
                .totalTrips(totalTrips)
                .recentBookingMonths(bookingMonths.isEmpty() ? null : bookingMonths.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByKey().reversed())
                        .limit(6)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)))
                .recentBookings(sortedBookings)
                .accommodationCount(accommodationCount)
                .accommodationTotalSpent(Math.round(accommodationTotalSpent))
                .tourCount(tourCount)
                .tourTotalSpent(Math.round(tourTotalSpent))
                .transportCount(transportCount)
                .transportTotalSpent(Math.round(transportTotalSpent))
                .build();
    }

    public Map<String, Object> getUserBookingSummary(UUID userId) {
        List<Booking> bookings = bookingRepository.findByUsers_IdUser(userId);
        
        int completedBookings = (int) bookings.stream()
                .filter(b -> "Success".equals(b.getStatus()) || "Checked Out".equals(b.getStatus()))
                .count();

        double totalSpent = bookings.stream()
                .filter(b -> "Success".equals(b.getStatus()) || "Checked Out".equals(b.getStatus()))
                .mapToDouble(b -> b.getTotalPrice() != null ? b.getTotalPrice().doubleValue() : 0)
                .sum();

        return Map.of(
            "completedBookings", completedBookings,
            "totalSpent", Math.round(totalSpent),
            "avgBookingValue", completedBookings > 0 ? Math.round(totalSpent / completedBookings) : 0
        );
    }
}