package com.example.java_service.Helpers;

import com.example.java_service.Dto.Response.*;
import com.example.java_service.Repositories.Jpa.*;
import com.example.java_service.Services.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SchedulingTools {

    AccommodationRepository accommodationRepository;
    TourRepository tourRepository;
    TransportTicketRepository transportTicketRepository;
    ServiceRepository serviceRepository;
    RoomRepository roomRepository;
    ObjectMapper objectMapper;

    private static final Map<String, String> TICKET_TYPE_KEYWORDS = new HashMap<>();
    static {
        TICKET_TYPE_KEYWORDS.put("máy bay", "Plane");
        TICKET_TYPE_KEYWORDS.put("plane", "Plane");
        TICKET_TYPE_KEYWORDS.put("airplane", "Plane");
        TICKET_TYPE_KEYWORDS.put("xe khách", "Coach");
        TICKET_TYPE_KEYWORDS.put("coach", "Coach");
        TICKET_TYPE_KEYWORDS.put("bus", "Coach");
        TICKET_TYPE_KEYWORDS.put("tàu hỏa", "Train");
        TICKET_TYPE_KEYWORDS.put("train", "Train");
        TICKET_TYPE_KEYWORDS.put("tàu cao tốc", "PassengerShip");
        TICKET_TYPE_KEYWORDS.put("tàu thuyền", "PassengerShip");
        TICKET_TYPE_KEYWORDS.put("passengership", "PassengerShip");
    }

    public String searchAccommodationIds(String destination) {
            try {
                String normalizedDestination = normalizeSearchTerm(destination);
                log.debug("Searching accommodations with normalized term: '{}'", normalizedDestination);
                
                List<com.example.java_service.Models.Accommodation> accommodations = accommodationRepository
                        .findByCityContainingIgnoreCaseAndApprovedTrue(normalizedDestination);
                log.debug("Search by city (approved): found {} results", accommodations.size());
                
                if (accommodations.isEmpty()) {
                    accommodations = accommodationRepository.findByProvinceContainingIgnoreCaseAndApprovedTrue(normalizedDestination);
                    log.debug("Search by province (approved): found {} results", accommodations.size());
                }
                if (accommodations.isEmpty()) {
                    accommodations = accommodationRepository.findByCityContainingIgnoreCase(normalizedDestination);
                    log.debug("Search by city (all): found {} results", accommodations.size());
                }
                if (accommodations.isEmpty()) {
                    accommodations = accommodationRepository.findByProvinceContainingIgnoreCase(normalizedDestination);
                    log.debug("Search by province (all): found {} results", accommodations.size());
                }
                if (accommodations.isEmpty()) {
                    accommodations = accommodationRepository.findByAccommodationNameContainingIgnoreCase(normalizedDestination);
                    log.debug("Search by name: found {} results", accommodations.size());
                }
                if (accommodations.isEmpty()) {
                    accommodations = accommodationRepository.findAll();
                    log.debug("Fallback - getting all accommodations: found {} results", accommodations.size());
                }
                
                List<UUID> ids = accommodations.stream()
                        .map(com.example.java_service.Models.Accommodation::getIdAccommodation)
                        .collect(Collectors.toList());
                log.info("Found {} accommodations for destination: {}", ids.size(), destination);
                return objectMapper.writeValueAsString(ids);
            } catch (Exception e) {
                log.error("Tool Error - searchAccommodationIds: {}", e.getMessage());
                return "[]";
            }
        }

    public String searchTourIds(String keyword) {
            try {
                String normalizedKeyword = normalizeSearchTerm(keyword);
                log.debug("Searching tours with normalized term: '{}'", normalizedKeyword);
                
                List<com.example.java_service.Models.Tour> tours = tourRepository
                        .findByTourNameContainingIgnoreCaseAndApprovedTrue(normalizedKeyword);
                log.debug("Search by tour name (approved): found {} results", tours.size());
                
                if (tours.isEmpty()) {
                    tours = tourRepository.findByDescriptionContainingIgnoreCaseAndApprovedTrue(normalizedKeyword);
                    log.debug("Search by description (approved): found {} results", tours.size());
                }
                if (tours.isEmpty()) {
                    tours = tourRepository.findByTourNameContainingIgnoreCase(normalizedKeyword);
                    log.debug("Search by tour name (all): found {} results", tours.size());
                }
                if (tours.isEmpty()) {
                    tours = tourRepository.findByDescriptionContainingIgnoreCase(normalizedKeyword);
                    log.debug("Search by description (all): found {} results", tours.size());
                }
                
                List<UUID> ids = tours.stream()
                        .map(com.example.java_service.Models.Tour::getIdTour)
                        .collect(Collectors.toList());
                log.info("Found {} tours for keyword: {} (NO FALLBACK - only matching tours)", ids.size(), keyword);
                return objectMapper.writeValueAsString(ids);
            } catch (Exception e) {
                log.error("Tool Error - searchTourIds: {}", e.getMessage());
                return "[]";
            }
        }

    public String searchTransportIds(String from, String to) {
            try {
                String normalizedFrom = normalizeSearchTerm(from);
                String normalizedTo = normalizeSearchTerm(to);
                log.debug("Searching transport with normalized terms: '{}' -> '{}'", normalizedFrom, normalizedTo);
                
                List<com.example.java_service.Models.TransportTicket> tickets = transportTicketRepository
                        .findByDepartureLocationContainingIgnoreCaseAndApprovedTrue(normalizedFrom);
                if (tickets.isEmpty()) {
                    tickets = transportTicketRepository.findByArrivalLocationContainingIgnoreCaseAndApprovedTrue(normalizedTo);
                }
                if (tickets.isEmpty()) {
                    tickets = transportTicketRepository.findByDepartureLocationContainingIgnoreCase(normalizedFrom);
                }
                if (tickets.isEmpty()) {
                    tickets = transportTicketRepository.findByArrivalLocationContainingIgnoreCase(normalizedTo);
                }
    
                List<com.example.java_service.Models.TransportTicket> filtered = tickets.stream()
                        .filter(t -> matchesDestination(t, normalizedFrom, normalizedTo))
                        .collect(Collectors.toList());
    
                List<UUID> ids = filtered.stream()
                        .map(com.example.java_service.Models.TransportTicket::getIdTicket)
                        .collect(Collectors.toList());
                log.info("Found {} transport tickets for route: {} -> {}", ids.size(), from, to);
                return objectMapper.writeValueAsString(ids);
            } catch (Exception e) {
                log.error("Tool Error - searchTransportIds: {}", e.getMessage());
                return "[]";
            }
        }

    public String searchRestaurantIds(String city) {
            try {
                String normalizedCity = normalizeSearchTerm(city);
                log.debug("Searching restaurants with normalized term: '{}'", normalizedCity);
                
                List<com.example.java_service.Models.Service> services = serviceRepository
                        .findByTypeService_TypeServiceIgnoreCase("restaurant");
    
                if (services.isEmpty()) {
                    return "[]";
                }
    
                List<UUID> ids = services.stream()
                        .filter(s -> s.getAccommodation() != null && (
                                (s.getAccommodation().getCity() != null &&
                                 s.getAccommodation().getCity().toLowerCase().contains(normalizedCity)) ||
                                (s.getAccommodation().getProvince() != null &&
                                 s.getAccommodation().getProvince().toLowerCase().contains(normalizedCity))))
                        .limit(10)
                        .map(com.example.java_service.Models.Service::getIdService)
                        .collect(Collectors.toList());
    
                log.info("Found {} restaurants for city: {}", ids.size(), city);
                return objectMapper.writeValueAsString(ids);
            } catch (Exception e) {
                log.error("Tool Error - searchRestaurantIds: {}", e.getMessage());
                return "[]";
            }
        }

    public String searchActivityIds(String city) {
            try {
                String normalizedCity = normalizeSearchTerm(city);
                log.debug("Searching activities with normalized term: '{}'", normalizedCity);
                
                List<com.example.java_service.Models.Service> services = serviceRepository
                        .findByTypeService_TypeServiceIgnoreCase("activity");
    
                if (services.isEmpty()) {
                    return "[]";
                }
    
                List<UUID> ids = services.stream()
                        .filter(s -> s.getAccommodation() != null && (
                                (s.getAccommodation().getCity() != null &&
                                 s.getAccommodation().getCity().toLowerCase().contains(normalizedCity)) ||
                                (s.getAccommodation().getProvince() != null &&
                                 s.getAccommodation().getProvince().toLowerCase().contains(normalizedCity))))
                        .limit(10)
                        .map(com.example.java_service.Models.Service::getIdService)
                        .collect(Collectors.toList());
    
                log.info("Found {} activities for city: {}", ids.size(), city);
                return objectMapper.writeValueAsString(ids);
            } catch (Exception e) {
                log.error("Tool Error - searchActivityIds: {}", e.getMessage());
                return "[]";
            }
        }

    public String getAccommodationDetails(String idAccommodation) {
            try {
                UUID uuid = UUID.fromString(idAccommodation);
                com.example.java_service.Models.Accommodation acc = accommodationRepository.findById(uuid).orElse(null);
                if (acc == null) {
                    return "{}";
                }
    
                List<com.example.java_service.Models.Room> rooms = roomRepository
                        .findByAccommodation_IdAccommodation(acc.getIdAccommodation());
                BigDecimal minPrice = rooms.stream().map(com.example.java_service.Models.Room::getRoomPrice).min(BigDecimal::compareTo).orElse(null);
                BigDecimal maxPrice = rooms.stream().map(com.example.java_service.Models.Room::getRoomPrice).max(BigDecimal::compareTo).orElse(null);
    
                AccommodationForScheduleResponse response = AccommodationForScheduleResponse.builder()
                        .idAccommodation(acc.getIdAccommodation())
                        .accommodationName(acc.getAccommodationName())
                        .city(acc.getCity())
                        .province(acc.getProvince())
                        .address(buildAddress(acc.getStreet(), acc.getWard(), acc.getCity()))
                        .minPrice(minPrice)
                        .maxPrice(maxPrice)
                        .build();
    
                return objectMapper.writeValueAsString(response);
            } catch (Exception e) {
                log.error("Tool Error - getAccommodationDetails: {}", e.getMessage());
                return "{}";
            }
        }

    public String getTourDetails(String idTour) {
            try {
                UUID uuid = UUID.fromString(idTour);
                com.example.java_service.Models.Tour tour = tourRepository.findById(uuid).orElse(null);
                if (tour == null) {
                    return "{}";
                }
    
                TourForScheduleResponse response = TourForScheduleResponse.builder()
                        .idTour(tour.getIdTour())
                        .tourName(tour.getTourName())
                        .description(tour.getDescription())
                        .durationMinutes(tour.getDurationMinutes())
                        .adultPrice(tour.getAdultPrice())
                        .childPrice(tour.getChildPrice())
                        .meetingPoint(tour.getMeetingPoint())
                        .status(tour.getStatus())
                        .build();
    
                return objectMapper.writeValueAsString(response);
            } catch (Exception e) {
                log.error("Tool Error - getTourDetails: {}", e.getMessage());
                return "{}";
            }
        }

    public String getTransportDetails(String idTicket) {
            try {
                UUID uuid = UUID.fromString(idTicket);
                com.example.java_service.Models.TransportTicket ticket = transportTicketRepository.findById(uuid).orElse(null);
                if (ticket == null) {
                    return "{}";
                }
    
                Integer durationMinutes = null;
                if (ticket.getDepartureTime() != null && ticket.getArrivalTime() != null) {
                    durationMinutes = (int) java.time.Duration.between(
                            ticket.getDepartureTime(), ticket.getArrivalTime()).toMinutes();
                }
    
                TransportForScheduleResponse response = TransportForScheduleResponse.builder()
                        .idTicket(ticket.getIdTicket())
                        .providerName(ticket.getProviderName())
                        .ticketType(ticket.getTicketType())
                        .departureLocation(ticket.getDepartureLocation())
                        .arrivalLocation(ticket.getArrivalLocation())
                        .price(ticket.getPrice())
                        .departureTime(ticket.getDepartureTime())
                        .arrivalTime(ticket.getArrivalTime())
                        .durationMinutes(durationMinutes)
                        .status(ticket.getStatus())
                        .build();
    
                return objectMapper.writeValueAsString(response);
            } catch (Exception e) {
                log.error("Tool Error - getTransportDetails: {}", e.getMessage());
                return "{}";
            }
        }

    public String getRestaurantDetails(String idService) {
            try {
                UUID uuid = UUID.fromString(idService);
                com.example.java_service.Models.Service service = serviceRepository.findById(uuid).orElse(null);
                if (service == null) {
                    return "{}";
                }
    
                String location = "";
                if (service.getAccommodation() != null) {
                    location = buildAddress(
                            service.getAccommodation().getStreet(),
                            service.getAccommodation().getWard(),
                            service.getAccommodation().getCity());
                }
    
                RestaurantForScheduleResponse response = RestaurantForScheduleResponse.builder()
                        .idService(service.getIdService())
                        .serviceName(service.getServiceName())
                        .typeService(service.getTypeService() != null ? service.getTypeService().getTypeService() : null)
                        .description(service.getDescription())
                        .servicePrice(service.getServicePrice())
                        .location(location)
                        .build();
    
                return objectMapper.writeValueAsString(response);
            } catch (Exception e) {
                log.error("Tool Error - getRestaurantDetails: {}", e.getMessage());
                return "{}";
            }
        }

    public String getActivityDetails(String idService) {
            try {
                UUID uuid = UUID.fromString(idService);
                com.example.java_service.Models.Service service = serviceRepository.findById(uuid).orElse(null);
                if (service == null) {
                    return "{}";
                }
    
                String location = "";
                if (service.getAccommodation() != null) {
                    location = buildAddress(
                            service.getAccommodation().getStreet(),
                            service.getAccommodation().getWard(),
                            service.getAccommodation().getCity());
                }
    
                ActivityForScheduleResponse response = ActivityForScheduleResponse.builder()
                        .idService(service.getIdService())
                        .serviceName(service.getServiceName())
                        .typeService(service.getTypeService() != null ? service.getTypeService().getTypeService() : null)
                        .description(service.getDescription())
                        .servicePrice(service.getServicePrice())
                        .location(location)
                        .build();
    
                return objectMapper.writeValueAsString(response);
            } catch (Exception e) {
                log.error("Tool Error - getActivityDetails: {}", e.getMessage());
                return "{}";
            }
        }

    private boolean matchesDestination(com.example.java_service.Models.TransportTicket ticket, String from, String to) {
        String depLoc = ticket.getDepartureLocation() != null ? ticket.getDepartureLocation().toLowerCase() : "";
        String arrLoc = ticket.getArrivalLocation() != null ? ticket.getArrivalLocation().toLowerCase() : "";
        String fromLower = from.toLowerCase();
        String toLower = to.toLowerCase();

        return (depLoc.contains(fromLower) || fromLower.contains(depLoc)) &&
               (arrLoc.contains(toLower) || toLower.contains(arrLoc));
    }

    private String buildAddress(String street, String ward, String city) {
        StringBuilder sb = new StringBuilder();
        if (street != null && !street.isEmpty()) sb.append(street);
        if (ward != null && !ward.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ward);
        }
        if (city != null && !city.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        return sb.toString();
    }

    public String findAccommodationFullInfo(String keyword, String destination) {
            try {
                String normalizedKeyword = normalizeSearchTerm(keyword);
                String normalizedDest = normalizeSearchTerm(destination);
                log.debug("Finding accommodation full info with keyword: '{}', destination: '{}'", normalizedKeyword, normalizedDest);
    
                List<com.example.java_service.Models.Accommodation> accommodations = new ArrayList<>();
    
                if (normalizedKeyword != null && !normalizedKeyword.isEmpty()) {
                    accommodations = accommodationRepository.findByAccommodationNameContainingIgnoreCaseAndApprovedTrue(normalizedKeyword);
                }
    
                if (accommodations.isEmpty() && normalizedDest != null && !normalizedDest.isEmpty()) {
                    accommodations = accommodationRepository.findByCityContainingIgnoreCaseAndApprovedTrue(normalizedDest);
                }
    
                if (accommodations.isEmpty()) {
                    accommodations = accommodationRepository.findByProvinceContainingIgnoreCaseAndApprovedTrue(normalizedDest);
                }
    
                if (accommodations.isEmpty()) {
                    accommodations = accommodationRepository.findByCityContainingIgnoreCase(normalizedDest);
                }
    
                if (accommodations.isEmpty()) {
                    log.info("No accommodations found for keyword: '{}', destination: '{}'", keyword, destination);
                    return "[]";
                }
    
                List<AccommodationWithRoomsAndServicesResult> results = new ArrayList<>();
                for (com.example.java_service.Models.Accommodation acc : accommodations.stream().limit(3).collect(Collectors.toList())) {
                    List<com.example.java_service.Models.Room> rooms = roomRepository.findByAccommodation_IdAccommodation(acc.getIdAccommodation());
                    List<com.example.java_service.Models.Service> services = serviceRepository.findByAccommodation_IdAccommodationAndStatus(
                            acc.getIdAccommodation(), "ACTIVE");
    
                    rooms = rooms.stream().filter(r -> "ACTIVE".equals(r.getStatus())).collect(Collectors.toList());
    
                    BigDecimal minPrice = rooms.stream().map(com.example.java_service.Models.Room::getRoomPrice).min(BigDecimal::compareTo).orElse(null);
                    BigDecimal maxPrice = rooms.stream().map(com.example.java_service.Models.Room::getRoomPrice).max(BigDecimal::compareTo).orElse(null);
    
                    List<RoomBasicResponse> roomResponses = rooms.stream().limit(10).map(room -> {
                        com.example.java_service.Models.TypeRoom typeRoom = room.getTypeRoom();
                        return RoomBasicResponse.builder()
                                .idRoom(room.getIdRoom())
                                .roomName(room.getRoomName())
                                .typeRoom(typeRoom != null ? typeRoom.getTypeRoom() : null)
                                .roomPrice(room.getRoomPrice())
                                .status(room.getStatus())
                                .build();
                    }).collect(Collectors.toList());
    
                    List<ServiceResponse> serviceResponses = services.stream().limit(20).map(svc -> {
                        return ServiceResponse.builder()
                                .idService(svc.getIdService())
                                .serviceName(svc.getServiceName())
                                .typeService(svc.getTypeService())
                                .servicePrice(svc.getServicePrice())
                                .status(svc.getStatus())
                                .build();
                    }).collect(Collectors.toList());
    
                    AccommodationWithRoomsAndServicesResult result = AccommodationWithRoomsAndServicesResult.builder()
                            .idAccommodation(acc.getIdAccommodation())
                            .accommodationName(acc.getAccommodationName())
                            .city(acc.getCity())
                            .province(acc.getProvince())
                            .address(buildAddress(acc.getStreet(), acc.getWard(), acc.getCity()))
                            .minPrice(minPrice)
                            .maxPrice(maxPrice)
                            .rooms(roomResponses)
                            .services(serviceResponses)
                            .build();
    
                    results.add(result);
                }
    
                log.info("Found {} accommodations with full info", results.size());
                return objectMapper.writeValueAsString(results);
            } catch (Exception e) {
                log.error("Tool Error - findAccommodationFullInfo: {}", e.getMessage());
                return "[]";
            }
        }

    public String findToursByDestinationAndBudget(String destination, BigDecimal maxBudget) {
            try {
                String normalizedDest = normalizeSearchTerm(destination);
                log.debug("Finding tours with destination: '{}', maxBudget: '{}'", normalizedDest, maxBudget);
    
                List<com.example.java_service.Models.Tour> tours = tourRepository
                        .findByTourNameContainingIgnoreCaseAndApprovedTrue(normalizedDest);
    
                if (tours.isEmpty()) {
                    tours = tourRepository.findByDescriptionContainingIgnoreCaseAndApprovedTrue(normalizedDest);
                }
    
                if (tours.isEmpty()) {
                    tours = tourRepository.findByTourNameContainingIgnoreCase(normalizedDest);
                }
    
                final BigDecimal budget = maxBudget != null ? maxBudget : BigDecimal.valueOf(Long.MAX_VALUE);
    
                List<com.example.java_service.Models.Tour> filteredTours = tours.stream()
                        .filter(t -> t.getAdultPrice() != null && t.getAdultPrice().compareTo(budget) <= 0)
                        .sorted((t1, t2) -> {
                            BigDecimal p1 = t1.getAdultPrice() != null ? t1.getAdultPrice() : BigDecimal.ZERO;
                            BigDecimal p2 = t2.getAdultPrice() != null ? t2.getAdultPrice() : BigDecimal.ZERO;
                            BigDecimal diff1 = p1.subtract(budget).abs();
                            BigDecimal diff2 = p2.subtract(budget).abs();
                            return diff1.compareTo(diff2);
                        })
                        .limit(3)
                        .collect(Collectors.toList());
    
                List<UUID> ids = filteredTours.stream()
                        .map(com.example.java_service.Models.Tour::getIdTour)
                        .collect(Collectors.toList());
    
                log.info("Found {} tours for destination: '{}' within budget", ids.size(), destination);
                return objectMapper.writeValueAsString(ids);
            } catch (Exception e) {
                log.error("Tool Error - findToursByDestinationAndBudget: {}", e.getMessage());
                return "[]";
            }
        }

    public String findTransportByRoute(String from, String to, String ticketTypeKeyword, BigDecimal maxBudget) {
            try {
                String normalizedFrom = normalizeSearchTerm(from);
                String normalizedTo = normalizeSearchTerm(to);
                log.debug("Finding transport from '{}' to '{}', ticketType: '{}', maxBudget: '{}'", normalizedFrom, normalizedTo, ticketTypeKeyword, maxBudget);
    
                String ticketType = null;
                if (ticketTypeKeyword != null && !ticketTypeKeyword.isEmpty()) {
                    ticketType = parseTicketTypeFromKeyword(ticketTypeKeyword);
                }
    
                List<com.example.java_service.Models.TransportTicket> tickets = transportTicketRepository
                        .findByDepartureLocationContainingIgnoreCaseAndApprovedTrue(normalizedFrom);
    
                if (tickets.isEmpty()) {
                    tickets = transportTicketRepository.findByDepartureLocationContainingIgnoreCase(normalizedFrom);
                }
    
                final BigDecimal budget = maxBudget != null ? maxBudget : BigDecimal.valueOf(Long.MAX_VALUE);
                final String finalTicketType = ticketType;
    
                List<com.example.java_service.Models.TransportTicket> filtered = tickets.stream()
                        .filter(t -> matchesDestination(t, normalizedFrom, normalizedTo))
                        .filter(t -> finalTicketType == null || finalTicketType.equalsIgnoreCase(t.getTicketType()))
                        .filter(t -> t.getPrice() != null && t.getPrice().compareTo(budget) <= 0)
                        .sorted((t1, t2) -> {
                            BigDecimal p1 = t1.getPrice() != null ? t1.getPrice() : BigDecimal.ZERO;
                            BigDecimal p2 = t2.getPrice() != null ? t2.getPrice() : BigDecimal.ZERO;
                            BigDecimal diff1 = p1.subtract(budget).abs();
                            BigDecimal diff2 = p2.subtract(budget).abs();
                            return diff1.compareTo(diff2);
                        })
                        .limit(5)
                        .collect(Collectors.toList());
    
                List<UUID> ids = filtered.stream()
                        .map(com.example.java_service.Models.TransportTicket::getIdTicket)
                        .collect(Collectors.toList());
    
                log.info("Found {} transport tickets for route: {} -> {}", ids.size(), from, to);
                return objectMapper.writeValueAsString(ids);
            } catch (Exception e) {
                log.error("Tool Error - findTransportByRoute: {}", e.getMessage());
                return "[]";
            }
        }

    private String parseTicketTypeFromKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }
        String lower = keyword.toLowerCase();
        for (Map.Entry<String, String> entry : TICKET_TYPE_KEYWORDS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String normalizeSearchTerm(String term) {
        if (term == null || term.trim().isEmpty()) {
            return "";
        }
        String normalized = term.trim().toLowerCase();
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized;
    }
}