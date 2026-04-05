package com.example.java_service.Helpers;

import com.example.java_service.Dto.Response.*;
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

    // ==================== TOUR TOOLS ====================

    @Tool("Tìm kiếm tour du lịch theo tên, địa điểm hoặc từ khóa liên quan. Trả về danh sách tour phù hợp.")
    public String searchTours(String keyword)
    {
        try {
            List<TourOverviewResponse> tours = tourService.searchTours(keyword);
            if (tours.isEmpty()) {
                return "Không tìm thấy tour nào phù hợp với từ khóa: " + keyword;
            }
            return objectMapper.writeValueAsString(tours);
        } catch (Exception e) {
            log.error("Tool Error - searchTours: {}", e.getMessage());
            return "Không tìm được tour với từ khóa: " + keyword;
        }
    }

    @Tool("Lấy thông tin chi tiết của tour bao gồm giá vé, lịch trình, điểm tập trung và các thông tin khác.")
    public String getTourDetails(String idTour)
    {
        try {
            TourResponse tour = tourService.getTourDetail(UUID.fromString(idTour));
            return objectMapper.writeValueAsString(tour);
        } catch (Exception e) {
            log.error("Tool Error - getTourDetails: {}", e.getMessage());
            return "Hiện tại không lấy được thông tin tour này.";
        }
    }

    @Tool("Lấy lịch trình chi tiết theo ngày của một tour khi có tour ID.")
    public String getTourSchedule(String idTour)
    {
        try {
            TourResponse tour = tourService.getTourDetail(UUID.fromString(idTour));
            if (tour.getTourScheduleResponses() == null || tour.getTourScheduleResponses().isEmpty()) {
                return "Tour này chưa có lịch trình chi tiết.";
            }
            return objectMapper.writeValueAsString(tour.getTourScheduleResponses());
        } catch (Exception e) {
            log.error("Tool Error - getTourSchedule: {}", e.getMessage());
            return "Không lấy được lịch trình tour.";
        }
    }

    @Tool("Lấy danh sách tất cả tour đang hoạt động (đã được phê duyệt).")
    public String getAllTours()
    {
        try {
            List<TourOverviewResponse> tours = tourService.getListTourApproved();
            if (tours.isEmpty()) {
                return "Hiện tại không có tour nào.";
            }
            return objectMapper.writeValueAsString(tours);
        } catch (Exception e) {
            log.error("Tool Error - getAllTours: {}", e.getMessage());
            return "Không lấy được danh sách tour.";
        }
    }

    // ==================== ACCOMMODATION TOOLS ====================

    @Tool("Tìm kiếm nơi cư trú (khách sạn, homestay, resort) theo tên, thành phố, địa điểm hoặc tiện ích.")
    public String searchAccommodations(String keyword)
    {
        try {
            List<GetListAccommodationResponse> accommodations = accommodationService.searchAccommodations(keyword);
            if (accommodations.isEmpty()) {
                return "Không tìm thấy nơi cư trú nào phù hợp với từ khóa: " + keyword;
            }
            return objectMapper.writeValueAsString(accommodations);
        } catch (Exception e) {
            log.error("Tool Error - searchAccommodations: {}", e.getMessage());
            return "Không tìm được nơi cư trú với từ khóa: " + keyword;
        }
    }

    @Tool("Lấy thông tin chi tiết của nơi cư trú bao gồm phòng, tiện ích, đánh giá, quy định.")
    public String getAccommodationDetails(String idAccommodation)
    {
        try {
            GetDetailAccommodationResponse accommodation = accommodationService.getAccommodationDetail(
                    UUID.fromString(idAccommodation), null);
            return objectMapper.writeValueAsString(accommodation);
        } catch (Exception e) {
            log.error("Tool Error - getAccommodationDetails: {}", e.getMessage());
            return "Hiện tại không lấy được thông tin nơi cư trú này.";
        }
    }

    @Tool("Lấy danh sách tất cả nơi cư trú đang hoạt động (đã được phê duyệt).")
    public String getAllAccommodations()
    {
        try {
            List<GetListAccommodationResponse> accommodations = accommodationService.getListAccommodationApproved(null);
            if (accommodations.isEmpty()) {
                return "Hiện tại không có nơi cư trú nào.";
            }
            return objectMapper.writeValueAsString(accommodations);
        } catch (Exception e) {
            log.error("Tool Error - getAllAccommodations: {}", e.getMessage());
            return "Không lấy được danh sách nơi cư trú.";
        }
    }

    // ==================== ROOM TOOLS ====================

    @Tool("Kiểm tra danh sách phòng, loại phòng, giá và trạng thái của một nơi cư trú.")
    public String checkRoomAvailability(String idAccommodation)
    {
        try {
            List<GetRoomResponse> rooms = roomService.getListRoom(UUID.fromString(idAccommodation));
            if (rooms.isEmpty()) {
                return "Không có phòng nào tại nơi cư trú này.";
            }
            return objectMapper.writeValueAsString(rooms);
        } catch (Exception e) {
            log.error("Tool Error - checkRoomAvailability: {}", e.getMessage());
            return "Không lấy được thông tin phòng.";
        }
    }

    @Tool("Lấy thông tin chi tiết một phòng cụ thể bao gồm hình ảnh, giá, tiện ích.")
    public String getRoomDetails(String idRoom)
    {
        try {
            GetRoomResponse room = roomService.getInfRoom(UUID.fromString(idRoom));
            return objectMapper.writeValueAsString(room);
        } catch (Exception e) {
            log.error("Tool Error - getRoomDetails: {}", e.getMessage());
            return "Không lấy được thông tin phòng.";
        }
    }

    // ==================== SERVICE TOOLS ====================

    @Tool("Tìm kiếm dịch vụ của khách sạn (spa, gym, nhà hàng, đưa đón...) theo tên dịch vụ.")
    public String searchServices(String keyword)
    {
        try {
            List<ServiceResponse> services = serviceService.searchServices(keyword);
            if (services.isEmpty()) {
                return "Không tìm thấy dịch vụ nào phù hợp với từ khóa: " + keyword;
            }
            return objectMapper.writeValueAsString(services);
        } catch (Exception e) {
            log.error("Tool Error - searchServices: {}", e.getMessage());
            return "Không tìm được dịch vụ với từ khóa: " + keyword;
        }
    }

    @Tool("Lấy danh sách tất cả dịch vụ của một nơi cư trú cụ thể.")
    public String getServicesByAccommodation(String idAccommodation)
    {
        try {
            List<ServiceResponse> services = serviceService.getListService(UUID.fromString(idAccommodation));
            if (services.isEmpty()) {
                return "Nơi cư trú này chưa có dịch vụ nào.";
            }
            return objectMapper.writeValueAsString(services);
        } catch (Exception e) {
            log.error("Tool Error - getServicesByAccommodation: {}", e.getMessage());
            return "Không lấy được danh sách dịch vụ.";
        }
    }

    // ==================== VOUCHER TOOLS ====================

    @Tool("Tìm kiếm voucher (mã giảm giá, khuyến mãi) theo tên, mã code hoặc điều kiện.")
    public String searchVouchers(String keyword)
    {
        try {
            List<VoucherViewAllResponse> vouchers = voucherService.searchVouchers(keyword);
            if (vouchers.isEmpty()) {
                return "Không tìm thấy voucher nào phù hợp với từ khóa: " + keyword;
            }
            return objectMapper.writeValueAsString(vouchers);
        } catch (Exception e) {
            log.error("Tool Error - searchVouchers: {}", e.getMessage());
            return "Không tìm được voucher với từ khóa: " + keyword;
        }
    }

    @Tool("Lấy danh sách voucher của một nơi cư trú cụ thể.")
    public String getVouchersByAccommodation(String idAccommodation)
    {
        try {
            List<VoucherViewResponse> vouchers = voucherService.listVouchersApprovedByAccommodation(
                    UUID.fromString(idAccommodation), null);
            if (vouchers.isEmpty()) {
                return "Nơi cư trú này chưa có voucher nào.";
            }
            return objectMapper.writeValueAsString(vouchers);
        } catch (Exception e) {
            log.error("Tool Error - getVouchersByAccommodation: {}", e.getMessage());
            return "Không lấy được danh sách voucher.";
        }
    }

    @Tool("Lấy tất cả voucher đang hoạt động trong hệ thống.")
    public String getAllActiveVouchers()
    {
        try {
            List<VoucherViewAllResponse> vouchers = voucherService.listVouchersApproved(null);
            if (vouchers.isEmpty()) {
                return "Hiện tại không có voucher nào đang hoạt động.";
            }
            return objectMapper.writeValueAsString(vouchers);
        } catch (Exception e) {
            log.error("Tool Error - getAllActiveVouchers: {}", e.getMessage());
            return "Không lấy được danh sách voucher.";
        }
    }

    // ==================== TRANSPORT TICKET TOOLS ====================

    @Tool("Tìm kiếm vé phương tiện di chuyển (máy bay, xe khách, tàu hỏa, tàu cao tốc) theo từ khóa.")
    public String searchTransportTickets(String keyword)
    {
        try {
            List<TransportTicketOverviewResponse> tickets = transportTicketService.searchTransportTickets(keyword);
            if (tickets.isEmpty()) {
                return "Không tìm thấy vé di chuyển nào phù hợp với từ khóa: " + keyword;
            }
            return objectMapper.writeValueAsString(tickets);
        } catch (Exception e) {
            log.error("Tool Error - searchTransportTickets: {}", e.getMessage());
            return "Không tìm được vé di chuyển với từ khóa: " + keyword;
        }
    }

    @Tool("Lấy thông tin chi tiết một vé di chuyển cụ thể.")
    public String getTransportTicketDetails(String idTicket)
    {
        try {
            TransportTicketResponse ticket = transportTicketService.getTransportTicketDetail(UUID.fromString(idTicket));
            return objectMapper.writeValueAsString(ticket);
        } catch (Exception e) {
            log.error("Tool Error - getTransportTicketDetails: {}", e.getMessage());
            return "Không lấy được thông tin vé.";
        }
    }

    @Tool("Lấy danh sách tất cả vé di chuyển đang hoạt động.")
    public String getAllTransportTickets()
    {
        try {
            List<TransportTicketOverviewResponse> tickets = transportTicketService.getListTransportTicketApproved();
            if (tickets.isEmpty()) {
                return "Hiện tại không có vé di chuyển nào.";
            }
            return objectMapper.writeValueAsString(tickets);
        } catch (Exception e) {
            log.error("Tool Error - getAllTransportTickets: {}", e.getMessage());
            return "Không lấy được danh sách vé.";
        }
    }

    // ==================== USER & BOOKING TOOLS ====================

    @Tool("Lấy thông tin tài khoản người dùng bao gồm điểm loyalty, thông tin cá nhân.")
    public String getUserProfile(String idUser)
    {
        try {
            UserResponse user = userService.getInfUser(UUID.fromString(idUser));
            return objectMapper.writeValueAsString(user);
        } catch (Exception e) {
            log.error("Tool Error - getUserProfile: {}", e.getMessage());
            return "Không lấy được thông tin người dùng.";
        }
    }

    @Tool("Lấy lịch sử điểm loyalty (tích điểm, tiêu điểm) của người dùng.")
    public String getLoyaltyHistory(String idUser)
    {
        try {
            List<LoyaltyTransactionResponse> history = loyaltyTransactionService.getHistoryByUser(UUID.fromString(idUser));
            if (history.isEmpty()) {
                return "Người dùng chưa có lịch sử điểm loyalty.";
            }
            return objectMapper.writeValueAsString(history);
        } catch (Exception e) {
            log.error("Tool Error - getLoyaltyHistory: {}", e.getMessage());
            return "Không lấy được lịch sử điểm.";
        }
    }

    @Tool("Lấy danh sách tour đã đặt của người dùng.")
    public String getUserTourBookings(String idUser)
    {
        try {
            List<BookingTourResponse> bookings = bookingTourService.getListBookingTourByUser(UUID.fromString(idUser));
            if (bookings.isEmpty()) {
                return "Người dùng chưa đặt tour nào.";
            }
            return objectMapper.writeValueAsString(bookings);
        } catch (Exception e) {
            log.error("Tool Error - getUserTourBookings: {}", e.getMessage());
            return "Không lấy được lịch sử đặt tour.";
        }
    }

    @Tool("Lấy danh sách vé di chuyển đã đặt của người dùng.")
    public String getUserTransportBookings(String idUser)
    {
        try {
            List<BookingTransportResponse> bookings = bookingTransportTicketService.getListBookingByUser(UUID.fromString(idUser));
            if (bookings.isEmpty()) {
                return "Người dùng chưa đặt vé di chuyển nào.";
            }
            return objectMapper.writeValueAsString(bookings);
        } catch (Exception e) {
            log.error("Tool Error - getUserTransportBookings: {}", e.getMessage());
            return "Không lấy được lịch sử đặt vé.";
        }
    }

    @Tool("Lấy chi tiết một đơn đặt tour cụ thể.")
    public String getBookingTourDetail(String idBookingTour)
    {
        try {
            BookingTourResponse booking = bookingTourService.getBookingTourDetail(UUID.fromString(idBookingTour));
            return objectMapper.writeValueAsString(booking);
        } catch (Exception e) {
            log.error("Tool Error - getBookingTourDetail: {}", e.getMessage());
            return "Không lấy được thông tin đặt tour.";
        }
    }

    @Tool("Lấy chi tiết một đơn đặt vé di chuyển cụ thể.")
    public String getBookingTransportDetail(String idBookingTransport)
    {
        try {
            BookingTransportResponse booking = bookingTransportTicketService.getBookingDetail(UUID.fromString(idBookingTransport));
            return objectMapper.writeValueAsString(booking);
        } catch (Exception e) {
            log.error("Tool Error - getBookingTransportDetail: {}", e.getMessage());
            return "Không lấy được thông tin đặt vé.";
        }
    }

    // ==================== FAVORITE & EVALUATE TOOLS ====================

    @Tool("Lấy danh sách nơi cư trú yêu thích của người dùng.")
    public String getUserFavoriteAccommodations(String idUser)
    {
        try {
            List<GetListAccommodationResponse> favorites = favoriteService.getListFavoriteAccommodation(idUser);
            if (favorites.isEmpty()) {
                return "Người dùng chưa có nơi cư trú yêu thích nào.";
            }
            return objectMapper.writeValueAsString(favorites);
        } catch (Exception e) {
            log.error("Tool Error - getUserFavoriteAccommodations: {}", e.getMessage());
            return "Không lấy được danh sách yêu thích.";
        }
    }

    @Tool("Lấy danh sách tour yêu thích của người dùng.")
    public String getUserFavoriteTours(String idUser)
    {
        try {
            List<TourOverviewResponse> favorites = favoriteService.getListFavoriteTour(idUser);
            if (favorites.isEmpty()) {
                return "Người dùng chưa có tour yêu thích nào.";
            }
            return objectMapper.writeValueAsString(favorites);
        } catch (Exception e) {
            log.error("Tool Error - getUserFavoriteTours: {}", e.getMessage());
            return "Không lấy được danh sách tour yêu thích.";
        }
    }

    @Tool("Lấy đánh giá của một nơi cư trú hoặc tour.")
    public String getEvaluates(String refId, String refType)
    {
        try {
            List<EvaluateResponse> evaluates = evaluatesService.getEvaluates(refId, refType);
            if (evaluates.isEmpty()) {
                return "Chưa có đánh giá nào cho mục này.";
            }
            return objectMapper.writeValueAsString(evaluates);
        } catch (Exception e) {
            log.error("Tool Error - getEvaluates: {}", e.getMessage());
            return "Không lấy được đánh giá.";
        }
    }

    // ==================== REGULATION & FACILITY TOOLS ====================

    @Tool("Lấy quy định và chính sách của một nơi cư trú (giờ check-in/out, quy định...).")
    public String getAccommodationRegulations(String idAccommodation)
    {
        try {
            GetDetailAccommodationResponse accommodation = accommodationService.getAccommodationDetail(
                    UUID.fromString(idAccommodation), null);
            if (accommodation.getGeneralRegulationResponses() == null || 
                accommodation.getGeneralRegulationResponses().isEmpty()) {
                return "Nơi cư trú này chưa có quy định cụ thể.";
            }
            return objectMapper.writeValueAsString(accommodation.getGeneralRegulationResponses());
        } catch (Exception e) {
            log.error("Tool Error - getAccommodationRegulations: {}", e.getMessage());
            return "Không lấy được quy định.";
        }
    }

    @Tool("Lấy danh sách tiện ích của một nơi cư trú (hồ bơi, spa, gym, nhà hàng...).")
    public String getAccommodationFacilities(String idAccommodation)
    {
        try {
            GetDetailAccommodationResponse accommodation = accommodationService.getAccommodationDetail(
                    UUID.fromString(idAccommodation), null);
            if (accommodation.getFacilityCategoryResponses() == null || 
                accommodation.getFacilityCategoryResponses().isEmpty()) {
                return "Nơi cư trú này chưa có thông tin tiện ích.";
            }
            return objectMapper.writeValueAsString(accommodation.getFacilityCategoryResponses());
        } catch (Exception e) {
            log.error("Tool Error - getAccommodationFacilities: {}", e.getMessage());
            return "Không lấy được thông tin tiện ích.";
        }
    }

    // ==================== PRICE COMPARE TOOLS ====================

    @Tool("So sánh giá phòng giữa các nơi cư trú. Input: danh sách ID nơi cư trú cách nhau bằng dấu phẩy.")
    public String compareRoomPrices(String idAccommodations)
    {
        try {
            String[] ids = idAccommodations.split(",");
            StringBuilder result = new StringBuilder("So sánh giá phòng:\n\n");

            for (String id : ids) {
                String trimmedId = id.trim();
                List<GetRoomResponse> rooms = roomService.getListRoom(UUID.fromString(trimmedId));
                if (!rooms.isEmpty()) {
                    GetRoomResponse cheapest = rooms.stream()
                            .min((r1, r2) -> r1.getRoomPrice().compareTo(r2.getRoomPrice()))
                            .orElse(rooms.get(0));
                    result.append("- ").append(trimmedId).append(": Giá thấp nhất ")
                          .append(cheapest.getRoomPrice()).append(" VNĐ\n");
                }
            }
            return result.toString();
        } catch (Exception e) {
            log.error("Tool Error - compareRoomPrices: {}", e.getMessage());
            return "Không so sánh được giá.";
        }
    }

    @Tool("Tìm nơi cư trú có điểm đánh giá cao nhất trong một thành phố.")
    public String findTopRatedAccommodations(String city)
    {
        try {
            List<GetListAccommodationResponse> accommodations = accommodationService.searchAccommodations(city);
            if (accommodations.isEmpty()) {
                return "Không tìm thấy nơi cư trú nào ở " + city;
            }
            List<GetListAccommodationResponse> sorted = accommodations.stream()
                    .filter(a -> a.getAverageStar() != null && a.getAverageStar() > 0)
                    .sorted((a, b) -> Double.compare(b.getAverageStar(), a.getAverageStar()))
                    .limit(5)
                    .toList();
            return objectMapper.writeValueAsString(sorted);
        } catch (Exception e) {
            log.error("Tool Error - findTopRatedAccommodations: {}", e.getMessage());
            return "Không tìm được kết quả.";
        }
    }

    @Tool("Tìm tour rẻ nhất hoặc có giá tốt nhất theo từ khóa.")
    public String findBestValueTours(String keyword)
    {
        try {
            List<TourOverviewResponse> tours = tourService.searchTours(keyword);
            if (tours.isEmpty()) {
                return "Không tìm thấy tour nào.";
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
            return "Không tìm được kết quả.";
        }
    }
}