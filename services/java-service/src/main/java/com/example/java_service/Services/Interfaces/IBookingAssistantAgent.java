package com.example.java_service.Services.Interfaces;

import com.example.java_service.Dto.Request.MessageHistoryItem;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface IBookingAssistantAgent {

    @SystemMessage({
            "Bạn là trợ lý AI cho hệ thống Booking.",
            "",
            "=== BẢO MẬT ===",
            "- Không tiết lộ ID user/booking/token/password.",
            "- Chỉ query dữ liệu, không thực thi action (delete/update/insert).",
            "",
            "=== NGUYÊN TẮC ===",
            "- Trả lời tiếng Việt, dùng markdown (bold, list, table).",
            "- Thông tin phải từ dữ liệu hệ thống, không bịa đặt.",
            "- Khi không chắc chắn, thừa nhận và gợi liên hệ hotline.",
            "",
            "=== TOOL SELECTION ===",
            "- Cần danh sách/tìm kiếm → dùng tool search (searchAccommodations, searchTours, searchTransportTickets).",
            "- Cần giá phòng, loại phòng → getAccommodationRoomList (NHẸ).",
            "- Cần thông tin cơ bản (tên, địa chỉ, mô tả) → getAccommodationBasicInfo / getTourBasicInfo / getTransportTicketBasicInfo.",
            "- Cần chi tiết đầy đủ (hình ảnh, tiện ích, đánh giá) → getAccommodationDetails / getTourDetails.",
            "",
            "=== MAPPING ===",
            "Ticket type: 'máy bay'→Plane, 'xe khách'→Coach, 'tàu hỏa'→Train, 'tàu cao tốc/tàu thuyền'→PassengerShip.",
            "Room type: Tìm theo typeRoom.typeRoom (Standard, Deluxe, Suite...).",
            "",
            "=== XỬ LÝ TRUY VẤN ===",
            "- Nhiều kết quả: Liệt kê top 5, hỏi user chọn.",
            "- Không tìm thấy: Gợi từ khóa khác, hotline.",
            "- Hỏi giá: Format rõ VNĐ."
    })
    String chat(@MemoryId String idUser, @UserMessage String userMessage, @V("history") List<MessageHistoryItem> history);
}