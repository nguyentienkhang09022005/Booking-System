//package com.example.java_service.Services;
//
//import com.example.java_service.Constant.PredefinedRoleChattingAI;
//import com.example.java_service.Dto.Request.ChatRequest;
//import com.example.java_service.Dto.Request.MessageHistoryItem;
//import com.example.java_service.Dto.Request.SummarizeTheCommentRequest;
//import com.example.java_service.Dto.Response.*;
//import com.example.java_service.Exception.ApiException;
//import com.example.java_service.Exception.ErrorCode;
//import com.example.java_service.Models.Users;
//import com.example.java_service.Repositories.Jpa.AccommodationRepository;
//import com.example.java_service.Repositories.Jpa.UserRepository;
//import lombok.AccessLevel;
//import lombok.RequiredArgsConstructor;
//import lombok.experimental.FieldDefaults;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
//public class ChatWithAIService
//{
//    ChatHistoryService chatHistoryService;
//    GeminiService geminiService;
//    UserRepository userRepository;
//    RedisService redisService;
//    AccommodationService accommodationService;
//    AccommodationRepository accommodationRepository;
//
//    static final Long DURATION_HISTORY_CHAT_WITH_AI = 604800L;
//
//    public ChatResponse sendChatForAI(ChatRequest chatRequest)
//    {
//        Users user = userRepository.findById(UUID.fromString(chatRequest.getIdUser())).orElseThrow(
//                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
//        );
//
//        List<GetListAccommodationResponse> accommodations = redisService.getAccommodationsCached(user.getIdUser());
//
//        List<GetDetailAccommodationResponse> detailAccommodations = accommodations.stream()
//                .map(h -> redisService.getAccommodationDetailCached(h.getIdAccommodation(), user.getIdUser()))
//                .toList();
//
//        List<List<ServiceResponse>> services = accommodations.stream()
//                .map(h -> redisService.getServicesCached(h.getIdAccommodation()))
//                .toList();
//
//        List<List<VoucherViewResponse>> vouchers = accommodations.stream()
//                .map(h -> redisService.getVouchersCached(h.getIdAccommodation(), user.getIdUser()))
//                .toList();
//
//        String systemIntroduction = String.format("""
//        Bạn là **BookingNext**, trợ lý nơi cưu trú thông minh, chuyên nghiệp, thân thiện và trung lập.
//
//        Nhiệm vụ:
//        1. Dựa trên dữ liệu nơi cư trú đã được phê duyệt, bao gồm:
//           - Nơi cư trú: id, tên, địa chỉ, vị trí (tọa độ hoặc gần địa điểm nổi bật), số sao, mô tả tổng quan, đánh giá.
//           - Phòng: loại phòng, giá, tiện nghi, hình ảnh.
//           - Dịch vụ: các dịch vụ nổi bật (spa, gym, đưa đón, ăn sáng…)
//           - Voucher & ưu đãi: mã giảm giá, điều kiện sử dụng.
//
//        2. Khi người dùng hỏi:
//           - Tư vấn nơi cư trú phù hợp theo yêu cầu (giá tiền, vị trí, tiện nghi, chất lượng, ưu đãi)
//           - So sánh nhiều khách sạn nếu cần, nêu rõ ưu/nhược điểm
//           - Trả URL chi tiết nếu có trong dữ liệu JSON (URL được trả theo http://localhost:3000/hotel/*idHotel*) phần idHotel lấy từ database
//           - Tóm tắt review nếu yêu cầu
//
//        3. Bảo mật và an toàn:
//            - Không tự ý cung cấp thông tin của các user khác chỉ được trả lời các thông tin của user đang hỏi và trừ các
//            id, password cũng không được trả lời.
//            - Được phép dùng data cung cấp để tư vấn, nếu phát hiện user có hành vi gian lận hay cố hỏi 1 thông tin tuyệt mật
//            trong database thì từ chối nhẹ nhàng
//
//        4. Nguyên tắc trả lời:
//           - Thông tin rõ ràng, dễ hiểu, thân thiện
//           - Nếu nhiều lựa chọn, gợi ý theo thứ tự ưu tiên
//           - Nhấn mạnh yếu tố nổi bật phù hợp với yêu cầu người dùng
//           - Trả URL trực tiếp nếu có
//
//        Dữ liệu JSON khách sạn của user này:
//        - Thông tin user (ẩn thông tin nhạy cảm): %s
//        - Danh sách khách sạn: %s
//        - Chi tiết khách sạn: %s
//        - Dịch vụ của khách sạn: %s
//        - Voucher của khách sạn: %s
//
//        Hãy sử dụng **toàn bộ dữ liệu JSON trên** để:
//        - Tìm khách sạn phù hợp theo yêu cầu người dùng
//        - Phân tích giá, vị trí, dịch vụ, chất lượng phòng, an ninh, value-for-money
//        - So sánh khách sạn nếu user yêu cầu
//        - Trả kết quả chi tiết nhưng thân thiện, lịch sự và trung lập
//        """,
//                user,
//                accommodations,
//                detailAccommodations,
//                services,
//                vouchers
//        );
//
//        List<MessageHistoryItem> historyItems = chatHistoryService.getHistoryChat(chatRequest.getIdUser());
//        String aiMessage = geminiService.GenerateChatUserModelResponse(
//                systemIntroduction,
//                historyItems,
//                chatRequest.getUserMessage());
//
//        aiMessage = aiMessage.replace("\\n", "\n").replace("\\r", "");
//
//        // Lưu redis
//        chatHistoryService.saveChat(
//                chatRequest.getIdUser(),
//                new MessageHistoryItem(
//                        PredefinedRoleChattingAI.USER_ROLE,
//                        chatRequest.getUserMessage()
//                ),
//                DURATION_HISTORY_CHAT_WITH_AI
//        );
//        chatHistoryService.saveChat(
//                chatRequest.getIdUser(),
//                new MessageHistoryItem(
//                        PredefinedRoleChattingAI.MODEL_ROLE,
//                        aiMessage
//                ),
//                DURATION_HISTORY_CHAT_WITH_AI
//        );
//
//        return ChatResponse.builder()
//                .AiResponse(aiMessage)
//                .build();
//    }
//
//    public List<MessageHistoryItem> ListChatHistory(String idUser) {
//        userRepository.findById(UUID.fromString(idUser)).orElseThrow(
//                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
//        );
//
//        return chatHistoryService.getHistoryChat(idUser);
//    }
//
//    public ResponseEntity<String> deleteChatHistory(String idUser) {
//        userRepository.findById(UUID.fromString(idUser)).orElseThrow(
//                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
//        );
//
//        chatHistoryService.deleteHistoryChat(idUser);
//        return ResponseEntity.ok("Xóa lịch sử trò chuyện với AI thành công!");
//    }
//
//    public ChatResponse aiSummarizeTheComment(SummarizeTheCommentRequest summarizeTheCommentRequest)
//    {
//        Users user = userRepository.findById(UUID.fromString(summarizeTheCommentRequest.getIdUser().toString())).orElseThrow(
//                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
//        );
//
//        accommodationRepository.findById(UUID.fromString(summarizeTheCommentRequest.getIdAccommodation().toString())).orElseThrow(
//                () -> new ApiException(ErrorCode.ACCOMMODATION_NOT_FOUND)
//        );
//
//        GetDetailAccommodationResponse detailAccommodation = accommodationService.getAccommodationDetail(summarizeTheCommentRequest.getIdAccommodation(),
//                                                                         summarizeTheCommentRequest.getIdUser());
//
//        String systemIntroduction = String.format("""
//        Bạn là **BookingNext**, trợ lý AI chuyên phân tích và tóm tắt đánh giá khách sạn một cách chính xác, khách quan và bảo mật.
//
//        NHIỆM VỤ CHÍNH:
//        1. **Tóm tắt review của khách sạn** dựa trên dữ liệu JSON cung cấp:
//           - Đánh giá tổng thể.
//           - Các điểm mạnh thường được khen.
//           - Các điểm hạn chế thường bị phàn nàn.
//           - Tóm tắt ngắn gọn nhưng bao quát đúng bản chất.
//           - Không tự tạo dữ liệu, chỉ sử dụng đúng dữ liệu có trong JSON.
//
//        2. **Không tư vấn, không đề xuất khách sạn** trong chế độ này — chỉ tóm tắt đánh giá.
//
//        3. **An toàn & bảo mật dữ liệu:**
//           - Tuyệt đối không tiết lộ thông tin cá nhân người dùng hoặc người đánh giá.
//           - Không hiển thị các dữ liệu nhạy cảm (ID user, mật khẩu, token…).
//           - Chỉ phân tích review, không trả về bất kỳ dữ liệu hệ thống nội bộ nào.
//           - Nếu yêu cầu vượt ngoài phạm vi tóm tắt review → từ chối nhẹ nhàng.
//
//        4. **Nguyên tắc trả lời:**
//           - Ngắn gọn – rõ ràng – khách quan – không thổi phồng.
//           - Không bịa đặt đánh giá không tồn tại.
//           - Nếu dữ liệu review trống → trả lời: "Hiện tại khách sạn chưa có đánh giá từ người dùng."
//
//        DỮ LIỆU ĐẦU VÀO:
//        - Thông tin user (đã ẩn dữ liệu nhạy cảm): %s
//        - Chi tiết khách sạn (danh sách đánh giá gồm bình luận và sao): %s
//
//        Hãy tóm tắt **review** của khách sạn theo cấu trúc:
//        1. *Mức độ hài lòng chung*
//        2. *Điểm khách khen nhiều nhất*
//        3. *Điểm khách phàn nàn nhiều nhất*
//        4. *Kết luận ngắn gọn*
//        """,
//                user,
//                detailAccommodation
//        );
//
//        List<MessageHistoryItem> historyItems = new ArrayList<>();
//        String aiMessage = geminiService.GenerateChatUserModelResponse(
//                systemIntroduction,
//                historyItems,
//                "");
//
//        aiMessage = aiMessage.replace("\\n", "\n").replace("\\r", "");
//        return ChatResponse.builder()
//                .AiResponse(aiMessage)
//                .build();
//    }
//}
