package com.example.java_service.Helpers;

import com.example.java_service.Configs.RagConfig;
import com.example.java_service.Dto.Response.*;
import com.example.java_service.Repositories.Jpa.*;
import com.example.java_service.Services.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingChatTools
{
    RoomService roomService;
    TourService tourService;
    AccommodationService accommodationService;
    VoucherService voucherService;
    TransportTicketService transportTicketService;
    ServiceService serviceService;
    BookingTourService bookingTourService;
    BookingTransportTicketService bookingTransportTicketService;
    FavoriteService favoriteService;
    LoyaltyTransactionService loyaltyTransactionService;
    UserService userService;
    EvaluatesService evaluatesService;
    ObjectMapper objectMapper;

    AccommodationRepository accommodationRepository;
    TourRepository tourRepository;
    TransportTicketRepository transportTicketRepository;
    RoomRepository roomRepository;
    ServiceRepository serviceRepository;
    VoucherRepository voucherRepository;
    TypeServiceRepository typeServiceRepository;

    // ==================== RESOLVE HELPERS ====================

    private List<UUID> searchAccommodationIds(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }
        if (RagConfig.isValidUuid(input)) {
            return List.of(UUID.fromString(input));
        }
        List<com.example.java_service.Models.Accommodation> results = accommodationRepository
                .findByAccommodationNameContainingIgnoreCase(input);
        if (results.isEmpty()) {
            results = accommodationRepository.findByCityContainingIgnoreCaseAndApprovedTrue(input);
        }
        if (results.isEmpty()) {
            results = accommodationRepository.findByProvinceContainingIgnoreCaseAndApprovedTrue(input);
        }
        return results.stream().map(com.example.java_service.Models.Accommodation::getIdAccommodation).toList();
    }

    private List<UUID> searchTourIds(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }
        if (RagConfig.isValidUuid(input)) {
            return List.of(UUID.fromString(input));
        }
        List<com.example.java_service.Models.Tour> results = tourRepository
                .findByTourNameContainingIgnoreCaseAndApprovedTrue(input);
        if (results.isEmpty()) {
            results = tourRepository.findByDescriptionContainingIgnoreCaseAndApprovedTrue(input);
        }
        return results.stream().map(com.example.java_service.Models.Tour::getIdTour).toList();
    }

    private List<UUID> searchTransportTicketIds(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }
        if (RagConfig.isValidUuid(input)) {
            return List.of(UUID.fromString(input));
        }

        String normalizedInput = input.toLowerCase().trim();
        String ticketType = null;
        if (normalizedInput.contains("máy bay") || normalizedInput.equals("plane")) {
            ticketType = "Plane";
        } else if (normalizedInput.contains("xe khách") || normalizedInput.equals("coach")) {
            ticketType = "Coach";
        } else if (normalizedInput.contains("tàu hỏa") || normalizedInput.equals("train")) {
            ticketType = "Train";
        } else if (normalizedInput.contains("tàu cao tốc") || normalizedInput.contains("tàu thuyền") || normalizedInput.equals("passengership")) {
            ticketType = "PassengerShip";
        }

        List<com.example.java_service.Models.TransportTicket> results = transportTicketRepository
                .findByProviderNameContainingIgnoreCaseAndApprovedTrue(input);
        if (results.isEmpty() && ticketType != null) {
            results = transportTicketRepository.findByTicketTypeContainingIgnoreCaseAndApprovedTrue(ticketType);
        }
        if (results.isEmpty()) {
            results = transportTicketRepository.findByDepartureLocationContainingIgnoreCaseAndApprovedTrue(input);
        }
        if (results.isEmpty()) {
            results = transportTicketRepository.findByArrivalLocationContainingIgnoreCaseAndApprovedTrue(input);
        }
        return results.stream().map(com.example.java_service.Models.TransportTicket::getIdTicket).toList();
    }

    private List<UUID> searchRoomIds(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }
        if (RagConfig.isValidUuid(input)) {
            return List.of(UUID.fromString(input));
        }
        List<com.example.java_service.Models.Room> results = roomRepository.findByRoomNameContainingIgnoreCase(input);
        if (results.isEmpty()) {
            results = roomRepository.findByTypeRoom_TypeRoomContainingIgnoreCase(input);
        }
        return results.stream().map(com.example.java_service.Models.Room::getIdRoom).toList();
    }

    private List<UUID> searchServiceIds(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }
        if (RagConfig.isValidUuid(input)) {
            return List.of(UUID.fromString(input));
        }
        return serviceRepository.findByServiceNameContainingIgnoreCase(input)
                .stream()
                .map(com.example.java_service.Models.Service::getIdService)
                .toList();
    }

    private String buildAmbiguousError(String input, int count, String entityType) {
        return String.format("Từ khóa '%s' trả về %d %s. Vui lòng cung cấp tên cụ thể hơn hoặc sử dụng UUID.", input, count, entityType);
    }

    private String currentUserId() {
        String idUser = CurrentUserContext.get();
        return (idUser != null && !idUser.isBlank()) ? idUser : null;
    }

    private static final int SEARCH_LIMIT = 5;
    private static final int LIST_LIMIT = 10;

    // Giới hạn số phần tử serialize để tránh tràn context của LLM
    private String toListJson(List<?> list, int limit, boolean withMore)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        int total = list.size();
        String json = objectMapper.writeValueAsString(list.stream().limit(limit).toList());
        if (withMore && total > limit) {
            return "Tổng " + total + " kết quả, hiển thị " + limit + " đầu. Gợi ý user cung cấp từ khóa cụ thể hơn.\n" + json;
        }
        return json;
    }

    // ==================== TOUR TOOLS ====================

    @Tool("Tìm tour: tên, địa điểm. Trả về danh sách.")
    public String searchTours(String keyword)
    {
        try {
            List<TourOverviewResponse> tours = tourService.searchTours(keyword);
            if (tours.isEmpty()) {
                return "Không tìm thấy tour: " + keyword;
            }
            return toListJson(tours, SEARCH_LIMIT, true);
        } catch (Exception e) {
            log.error("Tool Error - searchTours: {}", e.getMessage());
            return "Lỗi tìm tour.";
        }
    }

    @Tool("Dùng khi chỉ cần tên/giá/thời lượng/điểm đón của tour. Nhẹ — ưu tiên dùng trước. Tham số: tên hoặc UUID tour.")
    public String getTourBasicInfo(String idTour)
    {
        try {
            List<UUID> ids = searchTourIds(idTour);
            if (ids.isEmpty()) {
                return "Không tìm thấy tour: " + idTour;
            }
            if (ids.size() > 1) {
                return buildAmbiguousError(idTour, ids.size(), "tour");
            }
            TourResponse tour = tourService.getTourDetail(ids.get(0));
            TourBasicInfoResponse basic = TourBasicInfoResponse.builder()
                    .idTour(tour.getIdTour())
                    .tourName(tour.getTourName())
                    .description(tour.getDescription())
                    .adultPrice(tour.getAdultPrice())
                    .childPrice(tour.getChildPrice())
                    .durationMinutes(tour.getDurationMinutes())
                    .meetingPoint(tour.getMeetingPoint())
                    .dropOffPoint(tour.getDropOffPoint())
                    .cancellationPolicy(tour.getCancellationPolicy())
                    .languages(tour.getLanguages())
                    .status(tour.getStatus())
                    .build();
            return objectMapper.writeValueAsString(basic);
        } catch (Exception e) {
            log.error("Tool Error - getTourBasicInfo: {}", e.getMessage());
            return "Lỗi lấy thông tin tour.";
        }
    }

    @Tool("Dùng khi user hỏi sâu về tour (lịch trình, bao gồm/không bao gồm, chính sách hủy). Nặng, chỉ gọi khi cần. Tham số: tên hoặc UUID tour.")
    public String getTourDetails(String idTour)
    {
        try {
            List<UUID> ids = searchTourIds(idTour);
            if (ids.isEmpty()) {
                return "Không tìm thấy tour: " + idTour;
            }
            if (ids.size() > 1) {
                return buildAmbiguousError(idTour, ids.size(), "tour");
            }
            TourResponse tour = tourService.getTourDetail(ids.get(0));
            return objectMapper.writeValueAsString(tour);
        } catch (Exception e) {
            log.error("Tool Error - getTourDetails: {}", e.getMessage());
            return "Lỗi lấy chi tiết tour.";
        }
    }

    @Tool("Tất cả tour đã duyệt.")
    public String getAllTours()
    {
        try {
            List<TourOverviewResponse> tours = tourService.getListTourApproved();
            if (tours.isEmpty()) {
                return "Không có tour nào.";
            }
            return toListJson(tours, LIST_LIMIT, true);
        } catch (Exception e) {
            log.error("Tool Error - getAllTours: {}", e.getMessage());
            return "Lỗi lấy danh sách tour.";
        }
    }

    // ==================== ACCOMMODATION TOOLS ====================

    @Tool("Tìm nơi cư trú: tên, thành phố, địa điểm.")
    public String searchAccommodations(String keyword)
    {
        try {
            List<GetListAccommodationResponse> accommodations = accommodationService.searchAccommodations(keyword);
            if (accommodations.isEmpty()) {
                return "Không tìm thấy nơi cư trú: " + keyword;
            }
            return toListJson(accommodations, SEARCH_LIMIT, true);
        } catch (Exception e) {
            log.error("Tool Error - searchAccommodations: {}", e.getMessage());
            return "Lỗi tìm nơi cư trú.";
        }
    }

    @Tool("Dùng khi chỉ cần tên/địa chỉ/mô tả nơi cư trú. Nhẹ — ưu tiên dùng trước. Tham số: tên hoặc UUID.")
    public String getAccommodationBasicInfo(String idAccommodation)
    {
        try {
            List<UUID> ids = searchAccommodationIds(idAccommodation);
            if (ids.isEmpty()) {
                return "Không tìm thấy nơi cư trú: " + idAccommodation;
            }
            if (ids.size() > 1) {
                return buildAmbiguousError(idAccommodation, ids.size(), "nơi cư trú");
            }
            AccommodationBasicInfoResponse info = accommodationService.getAccommodationBasicInfo(ids.get(0));
            return objectMapper.writeValueAsString(info);
        } catch (Exception e) {
            log.error("Tool Error - getAccommodationBasicInfo: {}", e.getMessage());
            return "Lỗi lấy thông tin cơ bản.";
        }
    }

    @Tool("Dùng khi user hỏi sâu về nơi cư trú (phòng, hình ảnh, tiện ích, quy định, đánh giá). Nặng, chỉ gọi khi cần. Tham số: tên hoặc UUID.")
    public String getAccommodationDetails(String idAccommodation)
    {
        try {
            List<UUID> ids = searchAccommodationIds(idAccommodation);
            if (ids.isEmpty()) {
                return "Không tìm thấy nơi cư trú: " + idAccommodation;
            }
            if (ids.size() > 1) {
                return buildAmbiguousError(idAccommodation, ids.size(), "nơi cư trú");
            }
            GetDetailAccommodationResponse accommodation = accommodationService.getAccommodationDetail(ids.get(0), null);
            return objectMapper.writeValueAsString(accommodation);
        } catch (Exception e) {
            log.error("Tool Error - getAccommodationDetails: {}", e.getMessage());
            return "Lỗi lấy chi tiết nơi cư trú.";
        }
    }

    @Tool("Tất cả nơi cư trú đã duyệt.")
    public String getAllAccommodations()
    {
        try {
            List<GetListAccommodationResponse> accommodations = accommodationService.getListAccommodationApproved(null);
            if (accommodations.isEmpty()) {
                return "Không có nơi cư trú nào.";
            }
            return toListJson(accommodations, LIST_LIMIT, true);
        } catch (Exception e) {
            log.error("Tool Error - getAllAccommodations: {}", e.getMessage());
            return "Lỗi lấy danh sách nơi cư trú.";
        }
    }

    // ==================== ROOM TOOLS ====================

    @Tool("Danh sách phòng (giá, loại, trạng thái) của một nơi cư trú. Tham số: tên hoặc UUID nơi cư trú.")
    public String getRoomsByAccommodation(String idAccommodation)
    {
        try {
            List<UUID> ids = searchAccommodationIds(idAccommodation);
            if (ids.isEmpty()) {
                return "Không tìm thấy nơi cư trú: " + idAccommodation;
            }
            if (ids.size() > 1) {
                return buildAmbiguousError(idAccommodation, ids.size(), "nơi cư trú");
            }
            List<GetRoomResponse> rooms = roomService.getListRoom(ids.get(0));
            if (rooms.isEmpty()) {
                return "Không có phòng.";
            }
            return objectMapper.writeValueAsString(rooms);
        } catch (Exception e) {
            log.error("Tool Error - getRoomsByAccommodation: {}", e.getMessage());
            return "Lỗi lấy danh sách phòng.";
        }
    }

    @Tool("Chi tiết phòng: idRoom (UUID hoặc tên phòng).")
    public String getRoomDetails(String idRoom)
    {
        try {
            List<UUID> ids = searchRoomIds(idRoom);
            if (ids.isEmpty()) {
                return "Không tìm thấy phòng: " + idRoom;
            }
            if (ids.size() > 1) {
                return buildAmbiguousError(idRoom, ids.size(), "phòng");
            }
            GetRoomResponse room = roomService.getInfRoom(ids.get(0));
            return objectMapper.writeValueAsString(room);
        } catch (Exception e) {
            log.error("Tool Error - getRoomDetails: {}", e.getMessage());
            return "Lỗi lấy chi tiết phòng.";
        }
    }

    // ==================== SERVICE TOOLS ====================

    @Tool("Tìm dịch vụ theo tên, mô tả hoặc loại (VD: spa, gym, buffet). Tham số: keyword.")
    public String searchServices(String keyword)
    {
        try {
            List<ServiceResponse> services = serviceService.searchServices(keyword);
            if (services.isEmpty()) {
                services = serviceService.searchServicesByType(keyword);
            }
            if (services.isEmpty()) {
                return "Không tìm thấy dịch vụ: " + keyword;
            }
            return toListJson(services, SEARCH_LIMIT, true);
        } catch (Exception e) {
            log.error("Tool Error - searchServices: {}", e.getMessage());
            return "Lỗi tìm dịch vụ.";
        }
    }

    @Tool("Chi tiết dịch vụ: idService (UUID hoặc tên).")
    public String getServiceDetails(String idService)
    {
        try {
            List<UUID> ids = searchServiceIds(idService);
            if (ids.isEmpty()) {
                return "Không tìm thấy dịch vụ: " + idService;
            }
            if (ids.size() > 1) {
                return buildAmbiguousError(idService, ids.size(), "dịch vụ");
            }
            ServiceDetailResponse service = serviceService.getServiceDetail(ids.get(0));
            return objectMapper.writeValueAsString(service);
        } catch (Exception e) {
            log.error("Tool Error - getServiceDetails: {}", e.getMessage());
            return "Lỗi lấy chi tiết dịch vụ.";
        }
    }

    @Tool("Dịch vụ theo nơi cư trú: idAccommodation.")
    public String getServicesByAccommodation(String idAccommodation)
    {
        try {
            List<UUID> ids = searchAccommodationIds(idAccommodation);
            if (ids.isEmpty()) {
                return "Không tìm thấy nơi cư trú: " + idAccommodation;
            }
            if (ids.size() > 1) {
                return buildAmbiguousError(idAccommodation, ids.size(), "nơi cư trú");
            }
            List<ServiceResponse> services = serviceService.getListService(ids.get(0));
            if (services.isEmpty()) {
                return "Nơi cư trú chưa có dịch vụ.";
            }
            return objectMapper.writeValueAsString(services);
        } catch (Exception e) {
            log.error("Tool Error - getServicesByAccommodation: {}", e.getMessage());
            return "Lỗi lấy danh sách dịch vụ.";
        }
    }

    // ==================== VOUCHER TOOLS ====================

    @Tool("Tìm voucher: tên, mã code, điều kiện.")
    public String searchVouchers(String keyword)
    {
        try {
            List<VoucherViewAllResponse> vouchers = voucherService.searchVouchers(keyword);
            if (vouchers.isEmpty()) {
                return "Không tìm thấy voucher: " + keyword;
            }
            return toListJson(vouchers, SEARCH_LIMIT, true);
        } catch (Exception e) {
            log.error("Tool Error - searchVouchers: {}", e.getMessage());
            return "Lỗi tìm voucher.";
        }
    }

    @Tool("Voucher theo nơi cư trú: idAccommodation.")
    public String getVouchersByAccommodation(String idAccommodation)
    {
        try {
            List<UUID> ids = searchAccommodationIds(idAccommodation);
            if (ids.isEmpty()) {
                return "Không tìm thấy nơi cư trú: " + idAccommodation;
            }
            if (ids.size() > 1) {
                return buildAmbiguousError(idAccommodation, ids.size(), "nơi cư trú");
            }
            List<VoucherViewResponse> vouchers = voucherService.listVouchersApprovedByAccommodation(ids.get(0), null);
            if (vouchers.isEmpty()) {
                return "Nơi cư trú chưa có voucher.";
            }
            return objectMapper.writeValueAsString(vouchers);
        } catch (Exception e) {
            log.error("Tool Error - getVouchersByAccommodation: {}", e.getMessage());
            return "Lỗi lấy voucher.";
        }
    }

    @Tool("Tất cả voucher đang hoạt động.")
    public String getAllActiveVouchers()
    {
        try {
            List<VoucherViewAllResponse> vouchers = voucherService.listVouchersApproved(null);
            if (vouchers.isEmpty()) {
                return "Không có voucher nào.";
            }
            return toListJson(vouchers, LIST_LIMIT, true);
        } catch (Exception e) {
            log.error("Tool Error - getAllActiveVouchers: {}", e.getMessage());
            return "Lỗi lấy voucher.";
        }
    }

    // ==================== TRANSPORT TICKET TOOLS ====================

    @Tool("Tìm vé: nhà cung cấp, điểm đi, điểm đến, loại (vé máy bay, xe khách, tàu).")
    public String searchTransportTickets(String keyword)
    {
        try {
            List<TransportTicketOverviewResponse> tickets = transportTicketService.searchTransportTickets(keyword);
            if (tickets.isEmpty()) {
                return "Không tìm thấy vé: " + keyword;
            }
            return toListJson(tickets, SEARCH_LIMIT, true);
        } catch (Exception e) {
            log.error("Tool Error - searchTransportTickets: {}", e.getMessage());
            return "Lỗi tìm vé.";
        }
    }

    @Tool("Dùng khi chỉ cần loại/hãng/giờ/giá vé. Nhẹ — ưu tiên dùng trước. Tham số: tên hãng hoặc UUID vé.")
    public String getTransportTicketBasicInfo(String idTicket)
    {
        try {
            List<UUID> ids = searchTransportTicketIds(idTicket);
            if (ids.isEmpty()) {
                return "Không tìm thấy vé: " + idTicket;
            }
            if (ids.size() > 1) {
                return buildAmbiguousError(idTicket, ids.size(), "vé");
            }
            TransportTicketResponse ticket = transportTicketService.getTransportTicketDetail(ids.get(0));
            TransportTicketBasicInfoResponse basic = TransportTicketBasicInfoResponse.builder()
                    .idTicket(ticket.getIdTicket())
                    .ticketType(ticket.getTicketType())
                    .ticketClass(ticket.getTicketClass())
                    .providerName(ticket.getProviderName())
                    .departureLocation(ticket.getDepartureLocation())
                    .arrivalLocation(ticket.getArrivalLocation())
                    .departureTime(ticket.getDepartureTime())
                    .arrivalTime(ticket.getArrivalTime())
                    .price(ticket.getPrice())
                    .status(ticket.getStatus())
                    .build();
            return objectMapper.writeValueAsString(basic);
        } catch (Exception e) {
            log.error("Tool Error - getTransportTicketBasicInfo: {}", e.getMessage());
            return "Lỗi lấy thông tin vé.";
        }
    }

    @Tool("Dùng khi user hỏi sâu về vé (hạng vé, chính sách, mô tả đầy đủ). Nặng, chỉ gọi khi cần. Tham số: tên hãng hoặc UUID.")
    public String getTransportTicketDetails(String idTicket)
    {
        try {
            List<UUID> ids = searchTransportTicketIds(idTicket);
            if (ids.isEmpty()) {
                return "Không tìm thấy vé: " + idTicket;
            }
            if (ids.size() > 1) {
                return buildAmbiguousError(idTicket, ids.size(), "vé");
            }
            TransportTicketResponse ticket = transportTicketService.getTransportTicketDetail(ids.get(0));
            return objectMapper.writeValueAsString(ticket);
        } catch (Exception e) {
            log.error("Tool Error - getTransportTicketDetails: {}", e.getMessage());
            return "Lỗi lấy chi tiết vé.";
        }
    }

    @Tool("Tất cả vé đã duyệt.")
    public String getAllTransportTickets()
    {
        try {
            List<TransportTicketOverviewResponse> tickets = transportTicketService.getListTransportTicketApproved();
            if (tickets.isEmpty()) {
                return "Không có vé nào.";
            }
            return toListJson(tickets, LIST_LIMIT, true);
        } catch (Exception e) {
            log.error("Tool Error - getAllTransportTickets: {}", e.getMessage());
            return "Lỗi lấy danh sách vé.";
        }
    }

    // ==================== USER & BOOKING TOOLS ====================

    @Tool("Thông tin tài khoản của user hiện tại đang đăng nhập. Không cần tham số.")
    public String getUserProfile()
    {
        String idUser = currentUserId();
        if (idUser == null) {
            return "Bạn cần đăng nhập để xem thông tin này.";
        }
        try {
            UserResponse user = userService.getInfUser(UUID.fromString(idUser));
            return objectMapper.writeValueAsString(user);
        } catch (Exception e) {
            log.error("Tool Error - getUserProfile: {}", e.getMessage());
            return "Lỗi lấy thông tin user.";
        }
    }

    @Tool("Lịch sử điểm thưởng (loyalty) của user hiện tại. Không cần tham số.")
    public String getLoyaltyHistory()
    {
        String idUser = currentUserId();
        if (idUser == null) {
            return "Bạn cần đăng nhập để xem thông tin này.";
        }
        try {
            List<LoyaltyTransactionResponse> history = loyaltyTransactionService.getHistoryByUser(UUID.fromString(idUser));
            if (history.isEmpty()) {
                return "Chưa có lịch sử điểm.";
            }
            return objectMapper.writeValueAsString(history);
        } catch (Exception e) {
            log.error("Tool Error - getLoyaltyHistory: {}", e.getMessage());
            return "Lỗi lấy lịch sử loyalty.";
        }
    }

    @Tool("Danh sách tour user hiện tại đã đặt. Không cần tham số.")
    public String getUserTourBookings()
    {
        String idUser = currentUserId();
        if (idUser == null) {
            return "Bạn cần đăng nhập để xem thông tin này.";
        }
        try {
            List<BookingTourResponse> bookings = bookingTourService.getListBookingTourByUser(UUID.fromString(idUser));
            if (bookings.isEmpty()) {
                return "Chưa đặt tour nào.";
            }
            return objectMapper.writeValueAsString(bookings);
        } catch (Exception e) {
            log.error("Tool Error - getUserTourBookings: {}", e.getMessage());
            return "Lỗi lấy lịch sử đặt tour.";
        }
    }

    @Tool("Danh sách vé user hiện tại đã đặt. Không cần tham số.")
    public String getUserTransportBookings()
    {
        String idUser = currentUserId();
        if (idUser == null) {
            return "Bạn cần đăng nhập để xem thông tin này.";
        }
        try {
            List<BookingTransportResponse> bookings = bookingTransportTicketService.getListBookingByUser(UUID.fromString(idUser));
            if (bookings.isEmpty()) {
                return "Chưa đặt vé nào.";
            }
            return objectMapper.writeValueAsString(bookings);
        } catch (Exception e) {
            log.error("Tool Error - getUserTransportBookings: {}", e.getMessage());
            return "Lỗi lấy lịch sử đặt vé.";
        }
    }

    @Tool("Chi tiết đặt tour: idBookingTour.")
    public String getBookingTourDetail(String idBookingTour)
    {
        try {
            BookingTourResponse booking = bookingTourService.getBookingTourDetail(UUID.fromString(idBookingTour));
            return objectMapper.writeValueAsString(booking);
        } catch (Exception e) {
            log.error("Tool Error - getBookingTourDetail: {}", e.getMessage());
            return "Lỗi lấy chi tiết đặt tour.";
        }
    }

    @Tool("Chi tiết đặt vé: idBookingTransport.")
    public String getBookingTransportDetail(String idBookingTransport)
    {
        try {
            BookingTransportResponse booking = bookingTransportTicketService.getBookingDetail(UUID.fromString(idBookingTransport));
            return objectMapper.writeValueAsString(booking);
        } catch (Exception e) {
            log.error("Tool Error - getBookingTransportDetail: {}", e.getMessage());
            return "Lỗi lấy chi tiết đặt vé.";
        }
    }

    // ==================== FAVORITE & EVALUATE TOOLS ====================

    @Tool("Nơi cư trú yêu thích của user hiện tại. Không cần tham số.")
    public String getUserFavoriteAccommodations()
    {
        String idUser = currentUserId();
        if (idUser == null) {
            return "Bạn cần đăng nhập để xem thông tin này.";
        }
        try {
            List<GetListAccommodationResponse> favorites = favoriteService.getListFavoriteAccommodation(idUser);
            if (favorites.isEmpty()) {
                return "Chưa có nơi cư trú yêu thích.";
            }
            return objectMapper.writeValueAsString(favorites);
        } catch (Exception e) {
            log.error("Tool Error - getUserFavoriteAccommodations: {}", e.getMessage());
            return "Lỗi lấy danh sách yêu thích.";
        }
    }

    @Tool("Tour yêu thích của user hiện tại. Không cần tham số.")
    public String getUserFavoriteTours()
    {
        String idUser = currentUserId();
        if (idUser == null) {
            return "Bạn cần đăng nhập để xem thông tin này.";
        }
        try {
            List<TourOverviewResponse> favorites = favoriteService.getListFavoriteTour(idUser);
            if (favorites.isEmpty()) {
                return "Chưa có tour yêu thích.";
            }
            return objectMapper.writeValueAsString(favorites);
        } catch (Exception e) {
            log.error("Tool Error - getUserFavoriteTours: {}", e.getMessage());
            return "Lỗi lấy tour yêu thích.";
        }
    }

    @Tool("Đánh giá: refId và refType (accommodation/tour).")
    public String getEvaluates(String refId, String refType)
    {
        try {
            List<EvaluateResponse> evaluates = evaluatesService.getEvaluates(refId, refType);
            if (evaluates.isEmpty()) {
                return "Chưa có đánh giá.";
            }
            return toListJson(evaluates, LIST_LIMIT, true);
        } catch (Exception e) {
            log.error("Tool Error - getEvaluates: {}", e.getMessage());
            return "Lỗi lấy đánh giá.";
        }
    }

    // ==================== PRICE COMPARE TOOLS ====================

    @Tool("So sánh giá: idAccommodations (comma-separated UUID hoặc tên).")
    public String compareRoomPrices(String idAccommodations)
    {
        try {
            String[] parts = idAccommodations.split(",");
            StringBuilder result = new StringBuilder("So sánh giá:\n\n");

            for (String part : parts) {
                String trimmed = part.trim();
                List<UUID> ids = searchAccommodationIds(trimmed);

                if (ids.isEmpty()) {
                    result.append("- ").append(trimmed).append(": Không tìm thấy\n");
                    continue;
                }

                if (ids.size() > 1) {
                    result.append("- ").append(trimmed).append(": ").append(buildAmbiguousError(trimmed, ids.size(), "nơi cư trú")).append("\n");
                    continue;
                }

                List<GetRoomResponse> rooms = roomService.getListRoom(ids.get(0));
                if (rooms.isEmpty()) {
                    result.append("- ").append(trimmed).append(": Không có phòng\n");
                } else {
                    GetRoomResponse cheapest = rooms.stream()
                            .min((r1, r2) -> r1.getRoomPrice().compareTo(r2.getRoomPrice()))
                            .orElse(rooms.get(0));
                    result.append("- ").append(trimmed).append(": Giá thấp nhất ")
                          .append(cheapest.getRoomPrice()).append(" VNĐ\n");
                }
            }
            return result.toString();
        } catch (Exception e) {
            log.error("Tool Error - compareRoomPrices: {}", e.getMessage());
            return "Lỗi so sánh giá.";
        }
    }

    @Tool("Top rated: city (thành phố).")
    public String findTopRatedAccommodations(String city)
    {
        try {
            List<GetListAccommodationResponse> accommodations = accommodationService.searchAccommodations(city);
            if (accommodations.isEmpty()) {
                return "Không tìm thấy nơi cư trú ở: " + city;
            }
            List<GetListAccommodationResponse> sorted = accommodations.stream()
                    .filter(a -> a.getAverageStar() != null && a.getAverageStar() > 0)
                    .sorted((a, b) -> Double.compare(b.getAverageStar(), a.getAverageStar()))
                    .limit(5)
                    .toList();
            return objectMapper.writeValueAsString(sorted);
        } catch (Exception e) {
            log.error("Tool Error - findTopRatedAccommodations: {}", e.getMessage());
            return "Lỗi tìm top rated.";
        }
    }

    @Tool("Tour giá tốt: keyword.")
    public String findBestValueTours(String keyword)
    {
        try {
            List<TourOverviewResponse> tours = tourService.searchTours(keyword);
            if (tours.isEmpty()) {
                return "Không tìm thấy tour: " + keyword;
            }
            List<TourOverviewResponse> sorted = tours.stream()
                    .sorted((a, b) -> {
                        if (a.getAdultPrice() == null || b.getAdultPrice() == null) return 0;
                        return a.getAdultPrice().compareTo(b.getAdultPrice());
                    })
                    .limit(5)
                    .toList();
            return objectMapper.writeValueAsString(sorted);
        } catch (Exception e) {
            log.error("Tool Error - findBestValueTours: {}", e.getMessage());
            return "Lỗi tìm tour giá tốt.";
        }
    }
}