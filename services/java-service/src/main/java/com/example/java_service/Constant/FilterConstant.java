package com.example.java_service.Constant;

public class FilterConstant {

    private FilterConstant() {}

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public static final String SORT_RELEVANCE = "relevance";

    public static class SortDirection {
        public static final String ASC = "ASC";
        public static final String DESC = "DESC";
    }

    public static class AccommodationSortBy {
        public static final String ACCOMMODATION_NAME = "accommodationName";
        public static final String CITY = "city";
        public static final String PROVINCE = "province";
        public static final String CREATED_AT = "createdAt";
        public static final String STAR_RATING = "starRating";
        public static final String GUEST_RATING = "guestRating";
        public static final String PRICE = "price";
        public static final String DISCOUNT_PERCENT = "discountPercent";

        public static final java.util.List<String> ALLOWED = java.util.Arrays.asList(
                ACCOMMODATION_NAME, CITY, PROVINCE, CREATED_AT, STAR_RATING, GUEST_RATING, PRICE, DISCOUNT_PERCENT
        );
    }

    public static class TourSortBy {
        public static final String TOUR_NAME = "tourName";
        public static final String ADULT_PRICE = "adultPrice";
        public static final String DURATION_MINUTES = "durationMinutes";
        public static final String CREATED_AT = "createdAt";
        public static final String RATING = "rating";
        public static final String DEPARTURE_DATE = "departureDate";

        public static final java.util.List<String> ALLOWED = java.util.Arrays.asList(
                TOUR_NAME, ADULT_PRICE, DURATION_MINUTES, CREATED_AT, RATING, DEPARTURE_DATE
        );
    }

    public static class TransportTicketSortBy {
        public static final String PROVIDER_NAME = "providerName";
        public static final String PRICE = "price";
        public static final String DEPARTURE_TIME = "departureTime";
        public static final String DURATION = "duration";
        public static final String RATING = "rating";

        public static final java.util.List<String> ALLOWED = java.util.Arrays.asList(
                PROVIDER_NAME, PRICE, DEPARTURE_TIME, DURATION, RATING
        );
    }

    public static class VoucherSortBy {
        public static final String NAME_VOUCHER = "nameVoucher";
        public static final String DISCOUNT_PERCENT = "discountPercent";
        public static final String START_DATE = "startDate";
        public static final String END_DATE = "endDate";
        public static final String QUANTITY = "quantity";
        public static final String CREATE_AT = "createAt";

        public static final java.util.List<String> ALLOWED = java.util.Arrays.asList(
                NAME_VOUCHER, DISCOUNT_PERCENT, START_DATE, END_DATE, QUANTITY, CREATE_AT
        );
    }

    public static class AccommodationType {
        public static final String HOTEL = "HOTEL";
        public static final String HOSTEL = "HOSTEL";
        public static final String RESORT = "RESORT";
        public static final String VILLA = "VILLA";
        public static final String APARTMENT = "APARTMENT";
        public static final String HOMESTAY = "HOMESTAY";
        public static final String MOTEL = "MOTEL";

        public static final java.util.List<String> ALL = java.util.Arrays.asList(
                HOTEL, HOSTEL, RESORT, VILLA, APARTMENT, HOMESTAY, MOTEL
        );
    }

    public static class TicketType {
        public static final String PLANE = "PLANE";
        public static final String BUS = "BUS";
        public static final String TRAIN = "TRAIN";
        public static final String FERRY = "FERRY";
        public static final String CAR_RENTAL = "CAR_RENTAL";

        public static final java.util.List<String> ALL = java.util.Arrays.asList(
                PLANE, BUS, TRAIN, FERRY, CAR_RENTAL
        );
    }

    public static class TicketClass {
        public static final String ECONOMY = "ECONOMY";
        public static final String BUSINESS = "BUSINESS";
        public static final String FIRST = "FIRST";
        public static final String PREMIUM_ECONOMY = "PREMIUM_ECONOMY";

        public static final java.util.List<String> ALL = java.util.Arrays.asList(
                ECONOMY, BUSINESS, FIRST, PREMIUM_ECONOMY
        );
    }

    public static class SponsorType {
        public static final String SYSTEM = "SYSTEM";
        public static final String BUSINESS = "BUSINESS";

        public static final java.util.List<String> ALL = java.util.Arrays.asList(
                SYSTEM, BUSINESS
        );
    }

    public static class ApplyTo {
        public static final String ALL = "ALL";
        public static final String ROOM = "ROOM";
        public static final String SERVICE = "SERVICE";
        public static final String TOUR = "TOUR";
        public static final String TICKET = "TICKET";

        public static final java.util.List<String> ALL_TYPES = java.util.Arrays.asList(
                ALL, ROOM, SERVICE, TOUR, TICKET
        );
    }

    public static class DurationType {
        public static final String HALF_DAY = "HALF_DAY";
        public static final String FULL_DAY = "FULL_DAY";
        public static final String MULTIPLE_DAYS = "MULTIPLE_DAYS";

        public static final java.util.List<String> ALL = java.util.Arrays.asList(
                HALF_DAY, FULL_DAY, MULTIPLE_DAYS
        );
    }

    public static class TourType {
        public static final String ADVENTURE = "ADVENTURE";
        public static final String CULTURAL = "CULTURAL";
        public static final String BEACH = "BEACH";
        public static final String MOUNTAIN = "MOUNTAIN";
        public static final String CITY_TOUR = "CITY_TOUR";
        public static final String ECOTOURISM = "ECOTOURISM";
        public static final String FOOD_TOUR = "FOOD_TOUR";

        public static final java.util.List<String> ALL = java.util.Arrays.asList(
                ADVENTURE, CULTURAL, BEACH, MOUNTAIN, CITY_TOUR, ECOTOURISM, FOOD_TOUR
        );
    }

    public static class Difficulty {
        public static final String EASY = "EASY";
        public static final String MODERATE = "MODERATE";
        public static final String CHALLENGING = "CHALLENGING";

        public static final java.util.List<String> ALL = java.util.Arrays.asList(
                EASY, MODERATE, CHALLENGING
        );
    }

    public static class Amenity {
        public static final String WIFI = "WIFI";
        public static final String POOL = "POOL";
        public static final String PARKING = "PARKING";
        public static final String GYM = "GYM";
        public static final String RESTAURANT = "RESTAURANT";
        public static final String SPA = "SPA";
        public static final String PET_FRIENDLY = "PET_FRIENDLY";
        public static final String AIR_CONDITIONING = "AIR_CONDITIONING";
        public static final String BREAKFAST = "BREAKFAST";
        public static final String ROOM_SERVICE = "ROOM_SERVICE";
        public static final String BAR = "BAR";
        public static final String BEACH_ACCESS = "BEACH_ACCESS";
        public static final String AIRPORT_SHUTTLE = "AIRPORT_SHUTTLE";
        public static final String KIDS_CLUB = "KIDS_CLUB";
        public static final String LAUNDRY = "LAUNDRY";

        public static final java.util.List<String> ALL = java.util.Arrays.asList(
                WIFI, POOL, PARKING, GYM, RESTAURANT, SPA, PET_FRIENDLY,
                AIR_CONDITIONING, BREAKFAST, ROOM_SERVICE, BAR, BEACH_ACCESS,
                AIRPORT_SHUTTLE, KIDS_CLUB, LAUNDRY
        );
    }

    public static class TransportAmenity {
        public static final String WIFI = "WIFI";
        public static final String POWER_OUTLET = "POWER_OUTLET";
        public static final String AIR_CONDITIONING = "AIR_CONDITIONING";
        public static final String FOOD = "FOOD";
        public static final String DRINKS = "DRINKS";
        public static final String ENTERTAINMENT = "ENTERTAINMENT";
        public static final String LUGGAGE = "LUGGAGE";
        public static final String USB_PORT = "USB_PORT";
        public static final String RECLINING_SEAT = "RECLINING_SEAT";

        public static final java.util.List<String> ALL = java.util.Arrays.asList(
                WIFI, POWER_OUTLET, AIR_CONDITIONING, FOOD, DRINKS,
                ENTERTAINMENT, LUGGAGE, USB_PORT, RECLINING_SEAT
        );
    }

    public static class RoomType {
        public static final String SINGLE = "SINGLE";
        public static final String DOUBLE = "DOUBLE";
        public static final String SUITE = "SUITE";
        public static final String FAMILY = "FAMILY";
        public static final String DELUXE = "DELUXE";
        public static final String STUDIO = "STUDIO";

        public static final java.util.List<String> ALL = java.util.Arrays.asList(
                SINGLE, DOUBLE, SUITE, FAMILY, DELUXE, STUDIO
        );
    }

    public static class BedType {
        public static final String SINGLE_BED = "SINGLE_BED";
        public static final String DOUBLE_BED = "DOUBLE_BED";
        public static final String QUEEN = "QUEEN";
        public static final String KING = "KING";
        public static final String BUNK_BED = "BUNK_BED";

        public static final java.util.List<String> ALL = java.util.Arrays.asList(
                SINGLE_BED, DOUBLE_BED, QUEEN, KING, BUNK_BED
        );
    }

    public static class BaggageAllowance {
        public static final String CARRY_ON = "CARRY_ON";
        public static final String CHECKED_15KG = "CHECKED_15KG";
        public static final String CHECKED_20KG = "CHECKED_20KG";
        public static final String CHECKED_23KG = "CHECKED_23KG";
        public static final String CHECKED_30KG = "CHECKED_30KG";

        public static final java.util.List<String> ALL = java.util.Arrays.asList(
                CARRY_ON, CHECKED_15KG, CHECKED_20KG, CHECKED_23KG, CHECKED_30KG
        );
    }
}