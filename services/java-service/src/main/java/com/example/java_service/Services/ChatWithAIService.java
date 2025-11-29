package com.example.java_service.Services;

import com.example.java_service.Constant.PredefinedRoleChattingAI;
import com.example.java_service.Dto.Request.ChatRequest;
import com.example.java_service.Dto.Request.MessageHistoryItem;
import com.example.java_service.Dto.Response.*;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatWithAIService
{
    ChatHistoryService chatHistoryService;
    GeminiService geminiService;
    UserRepository userRepository;
    RedisService redisService;

    static final Long DURATION_HISTORY_CHAT_WITH_AI = 604800L;

    public ChatResponse sendChatForAI(ChatRequest chatRequest)
    {
        Users user = userRepository.findById(UUID.fromString(chatRequest.getIdUser())).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        List<GetListHotelResponse> hotels = redisService.getHotelsCached(user.getIdUser());

        List<GetDetailHotelResponse> detailHotels = hotels.stream()
                .map(h -> redisService.getHotelDetailCached(h.getIdHotel(), user.getIdUser()))
                .toList();

        List<List<ServiceResponse>> services = hotels.stream()
                .map(h -> redisService.getServicesCached(h.getIdHotel()))
                .toList();

        List<List<VoucherViewResponse>> vouchers = hotels.stream()
                .map(h -> redisService.getVouchersCached(h.getIdHotel()))
                .toList();

        String systemIntroduction = String.format("""
        Bạn là **BookingNext**, trợ lý khách sạn thông minh, chuyên nghiệp, thân thiện và trung lập.
        
        Nhiệm vụ:
        1. Dựa trên dữ liệu khách sạn đã được phê duyệt, bao gồm:
           - Khách sạn: id, tên, địa chỉ, vị trí (tọa độ hoặc gần địa điểm nổi bật), số sao, mô tả tổng quan, đánh giá.
           - Phòng: loại phòng, giá, tiện nghi, hình ảnh.
           - Dịch vụ: các dịch vụ nổi bật (spa, gym, đưa đón, ăn sáng…)
           - Voucher & ưu đãi: mã giảm giá, điều kiện sử dụng.
        
        2. Khi người dùng hỏi:
           - Tư vấn khách sạn phù hợp theo yêu cầu (giá tiền, vị trí, tiện nghi, chất lượng, ưu đãi)
           - So sánh nhiều khách sạn nếu cần, nêu rõ ưu/nhược điểm
           - Trả URL chi tiết nếu có trong dữ liệu JSON (URL được trả theo http://localhost:3000/hotel/*idHotel*) phần idHotel lấy từ database
           - Tóm tắt review nếu yêu cầu
        
        3. Bảo mật và an toàn:
            - Không tự ý cung cấp thông tin của các user khác chỉ được trả lời các thông tin của user đang hỏi và trừ các
            id, password cũng không được trả lời.
            - Được phép dùng data cung cấp để tư vấn, nếu phát hiện user có hành vi gian lận hay cố hỏi 1 thông tin tuyệt mật
            trong database thì từ chối nhẹ nhàng
        
        4. Nguyên tắc trả lời:
           - Thông tin rõ ràng, dễ hiểu, thân thiện
           - Nếu nhiều lựa chọn, gợi ý theo thứ tự ưu tiên
           - Nhấn mạnh yếu tố nổi bật phù hợp với yêu cầu người dùng
           - Trả URL trực tiếp nếu có
        
        Dữ liệu JSON khách sạn của user này:
        - Thông tin user (ẩn thông tin nhạy cảm): %s
        - Danh sách khách sạn: %s
        - Chi tiết khách sạn: %s
        - Dịch vụ của khách sạn: %s
        - Voucher của khách sạn: %s
        
        Hãy sử dụng **toàn bộ dữ liệu JSON trên** để:
        - Tìm khách sạn phù hợp theo yêu cầu người dùng
        - Phân tích giá, vị trí, dịch vụ, chất lượng phòng, an ninh, value-for-money
        - So sánh khách sạn nếu user yêu cầu
        - Trả kết quả chi tiết nhưng thân thiện, lịch sự và trung lập
        """,
                user,
                hotels,
                detailHotels,
                services,
                vouchers
        );

        List<MessageHistoryItem> historyItems = chatHistoryService.getHistoryChat(chatRequest.getIdUser());
        String aiMessage = geminiService.GenerateChatUserModelResponse(
                systemIntroduction,
                historyItems,
                chatRequest.getUserMessage());

        aiMessage = aiMessage.replace("\\n", "\n").replace("\\r", "");

        // Lưu redis
        chatHistoryService.saveChat(
                chatRequest.getIdUser(),
                new MessageHistoryItem(
                        PredefinedRoleChattingAI.USER_ROLE,
                        chatRequest.getUserMessage()
                ),
                DURATION_HISTORY_CHAT_WITH_AI
        );
        chatHistoryService.saveChat(
                chatRequest.getIdUser(),
                new MessageHistoryItem(
                        PredefinedRoleChattingAI.MODEL_ROLE,
                        aiMessage
                ),
                DURATION_HISTORY_CHAT_WITH_AI
        );

        return ChatResponse.builder()
                .AiResponse(aiMessage)
                .build();
    }

    public List<MessageHistoryItem> ListChatHistory(String idUser) {
        userRepository.findById(UUID.fromString(idUser)).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        return chatHistoryService.getHistoryChat(idUser);
    }

    public ResponseEntity<String> deleteChatHistory(String idUser) {
        userRepository.findById(UUID.fromString(idUser)).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        chatHistoryService.deleteRefreshToken(idUser);
        return ResponseEntity.ok("Xóa lịch sử trò chuyện với AI thành công!");
    }
}
