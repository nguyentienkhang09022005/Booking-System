package com.example.java_service.Services;

import com.example.java_service.Configs.RabbitConfig;
import com.example.java_service.Constant.PredefinedRefFavoriteAndEvaluate;
import com.example.java_service.Constant.PredefinedTimeChangeEvaluate;
import com.example.java_service.Dto.Request.EvaluateCreationRequest;
import com.example.java_service.Dto.Request.EvaluateUpdateRequest;
//import com.example.java_service.Dto.Request.MessageHistoryItem;
import com.example.java_service.Dto.Response.EvaluateResponse;
//import com.example.java_service.Dto.Response.ModerationResult;
import com.example.java_service.Dto.Response.NotificationCreatedEvent;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.EvaluateMapper;
import com.example.java_service.Models.Evaluates;
import com.example.java_service.Models.Accommodation;
import com.example.java_service.Models.Tour;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.AccommodationRepository;
import com.example.java_service.Repositories.Jpa.TourRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import com.example.java_service.Repositories.Mongodb.EvaluatesRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EvaluatesService {

    EvaluatesRepository evaluatesRepository;
    AccommodationRepository accommodationRepository;
    TourRepository tourRepository; // Bổ sung repository của Tour
    UserRepository userRepository;
    //    GeminiService geminiService;
    SystemParameterService systemParameterService;
    ObjectMapper objectMapper;
    EvaluateMapper evaluateMapper;
    RabbitTemplate rabbitTemplate;
    RabbitConfig rabbitConfig;

    // Hàm helper để kiểm tra xem dịch vụ (Accommodation/Tour) có tồn tại không
    private void validateServiceExists(String refId, String refType) {
        if (PredefinedRefFavoriteAndEvaluate.REF_ACCOMMODATION.equalsIgnoreCase(refType)) {
            if (!accommodationRepository.existsById(UUID.fromString(refId))) {
                throw new ApiException(ErrorCode.ACCOMMODATION_NOT_FOUND);
            }
        } else if (PredefinedRefFavoriteAndEvaluate.REF_TOUR.equalsIgnoreCase(refType)) {
            if (!tourRepository.existsById(UUID.fromString(refId))) {
                throw new ApiException(ErrorCode.TOUR_NOT_FOUND);
            }
        } else {
            throw new ApiException(ErrorCode.INVALID_DATA); // Hoặc tạo mã lỗi REF_TYPE_INVALID
        }
    }

    // Hàm lấy danh sách đánh giá theo phân trang
    public Page<EvaluateResponse> getListEvaluateByPaging(Pageable pageable, String refId, String refType) {
        validateServiceExists(refId, refType);

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                3,
                Sort.by("createdAt").descending()
        );

        Page<Evaluates> evaluatesPage = evaluatesRepository.findByRefIdAndRefType(refId, refType, sortedPageable);

        List<EvaluateResponse> responses = evaluatesPage.getContent().stream()
                .map(evaluateMapper::toEvaluateResponse)
                .toList();

        return new PageImpl<>(responses, evaluatesPage.getPageable(), evaluatesPage.getTotalElements());
    }

    // Hàm tạo đánh giá
    public EvaluateResponse saveEvaluate(EvaluateCreationRequest request) {
        Users user = userRepository.findById(UUID.fromString(request.getIdUser())).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        UUID ownerId = null;
        String targetName = "";
        String targetTypeVi = "";

        // Kiểm tra loại dịch vụ và lấy thông tin gửi thông báo
        if (PredefinedRefFavoriteAndEvaluate.REF_ACCOMMODATION.equalsIgnoreCase(request.getRefType())) {
            Accommodation accommodation = accommodationRepository.findById(UUID.fromString(request.getRefId())).orElseThrow(
                    () -> new ApiException(ErrorCode.ACCOMMODATION_NOT_FOUND)
            );
            ownerId = accommodation.getUsers().getIdUser();
            targetName = accommodation.getAccommodationName();
            targetTypeVi = "nơi cư trú";

        } else if (PredefinedRefFavoriteAndEvaluate.REF_TOUR.equalsIgnoreCase(request.getRefType())) {
            Tour tour = tourRepository.findById(UUID.fromString(request.getRefId())).orElseThrow(
                    () -> new ApiException(ErrorCode.TOUR_NOT_FOUND)
            );
            ownerId = tour.getUsers().getIdUser();
            targetName = tour.getTourName();
            targetTypeVi = "tour";

        } else {
            throw new ApiException(ErrorCode.INVALID_DATA);
        }

        // Kiểm tra xem user đã đánh giá dịch vụ này chưa
        Boolean checkEvaluate = evaluatesRepository.existsByRefIdAndRefTypeAndIdUser(
                request.getRefId(), request.getRefType(), request.getIdUser()
        );
        if (checkEvaluate) {
            throw new ApiException(ErrorCode.USER_EVALUATED_ACCOMMODATION); // Có thể đổi tên Exception thành USER_EVALUATED_ALREADY
        }

//        ModerationResult moderation = moderate(request.getComment());
//
//        if (moderation.getStatus().equalsIgnoreCase("REJECTED")) {
//            throw new ApiException(ErrorCode.COMMENT_REJECTED_BY_AI, moderation.getReason());
//        }

        Evaluates evaluates = evaluateMapper.toEvaluate(request);
        evaluates.setCreatedAt(LocalDateTime.now());
        evaluatesRepository.save(evaluates);

        String fullName = String.join(" ", user.getFirstName(), user.getLastName());

        // Gửi sự kiện vào Exchanges
        NotificationCreatedEvent event = NotificationCreatedEvent.builder()
                .idNotification(UUID.randomUUID())
                .idUser(ownerId)
                .title("Thông báo đánh giá " + targetTypeVi)
                .content(String.format(
                        "Người dùng %s vừa đánh giá %s %s của bạn.",
                        fullName, targetTypeVi, targetName
                ))
                .type("evaluate")
                .idRef(evaluates.getId())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            String message = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(
                    rabbitConfig.EXCHANGE_NOTIFICATION,
                    rabbitConfig.ROUTING_KEY_NOTIFICATION,
                    message
            );
        } catch (Exception ex) {
            log.error("Lỗi gửi thông báo đánh giá", ex);
        }

        return evaluateMapper.toEvaluateResponse(evaluates);
    }

    // Hàm sửa đánh giá
    public EvaluateResponse updateEvaluate(EvaluateUpdateRequest request, String id) {
        Evaluates evaluate = evaluatesRepository.findById(id).orElseThrow(
                () -> new ApiException(ErrorCode.EVALUATE_NOT_FOUND)
        );

        int updateExpireMinutes = Integer.parseInt(systemParameterService.getParamValue("EVALUATE_UPDATE_EXPIRE_MINUTES", PredefinedTimeChangeEvaluate.TIME_CHANGE));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = evaluate.getCreatedAt().plusMinutes(updateExpireMinutes);

        if (now.isAfter(deadline)) {
            throw new ApiException(ErrorCode.EVALUATE_UPDATE_TIME_EXPIRED);
        }

//        ModerationResult moderation = moderate(request.getComment());
//
//        if (moderation.getStatus().equalsIgnoreCase("REJECTED")) {
//            throw new ApiException(ErrorCode.COMMENT_REJECTED_BY_AI, moderation.getReason());
//        }

        evaluateMapper.updateEvaluateFromRequest(request, evaluate);
        evaluatesRepository.save(evaluate);
        return evaluateMapper.toEvaluateResponse(evaluate);
    }

    // Hàm xóa đánh giá
    public ResponseEntity<String> deleteEvaluate(String id) {
        Evaluates evaluate = evaluatesRepository.findById(id).orElseThrow(
                () -> new ApiException(ErrorCode.EVALUATE_NOT_FOUND)
        );
        evaluatesRepository.delete(evaluate);
        return ResponseEntity.ok("Xóa đánh giá thành công!");
    }

    // Hàm lấy tất cả số lượng đánh giá của dịch vụ
    public Integer getAllEvaluate(String refId, String refType) {
        validateServiceExists(refId, refType);
        return (int) evaluatesRepository.countByRefIdAndRefType(refId, refType);
    }

    // Hàm lấy trung bình số sao của dịch vụ
    public Double getAverageStar(String refId, String refType) {
        validateServiceExists(refId, refType);
        Double average = evaluatesRepository.getAverageRatingByRefIdAndRefType(refId, refType);
        return average != null ? average : 0.0;
    }

    public List<EvaluateResponse> getEvaluates(String refId, String refType) {
        validateServiceExists(refId, refType);
        List<Evaluates> evaluates = evaluatesRepository.findTop3ByRefIdAndRefTypeOrderByCreatedAtAsc(refId, refType);
        return evaluates.stream()
                .map(evaluateMapper::toEvaluateResponse)
                .toList();
    }

    // AI kiểm duyệt nội dung (Đã cập nhật prompt để dùng chung cho mọi dịch vụ)
//    public String automaticNegativeCommentModeration(String comment) {
//        String systemIntroduction = String.format("""
//        Bạn là hệ thống kiểm duyệt bình luận **BookingNext Moderation AI**, có nhiệm vụ phân tích mức độ phù hợp của bình luận do người dùng gửi lên. Đây là chế độ kiểm duyệt nghiêm ngặt, không được tư vấn dịch vụ, không được phân tích dữ liệu nội bộ, không được tiết lộ thông tin người dùng.
//
//        NHIỆM VỤ:
//        1. Phân tích nội dung bình luận và phân loại vào đúng một trong các trạng thái:
//           - **APPROVED**: Bình luận hợp lệ, lịch sự, không chứa nội dung nhạy cảm hoặc tiêu cực nghiêm trọng.
//           - **REJECTED**: Bình luận chứa các nội dung bị cấm:
//             * Ngôn từ xúc phạm, chửi thề, đe doạ, khiêu khích
//             * Ngôn ngữ độc hại hoặc phân biệt đối xử
//             * Spam / quảng cáo / đường dẫn độc hại
//             * Tiết lộ thông tin cá nhân (số điện thoại, email, CCCD…)
//             * Nội dung liên quan tấn công hệ thống (SQL injection, mã độc…)
//             * Nội dung vi phạm pháp luật hoặc tục tĩu
//
//        2. PHẢI trả về kết quả dưới dạng JSON **duy nhất**, đúng mẫu sau:
//        {
//          "status": "APPROVED | REJECTED",
//          "reason": "Giải thích ngắn gọn và chính xác vì sao bình luận được phân loại như vậy"
//        }
//
//        3. Quy tắc nghiêm ngặt:
//           - Không được tạo thêm văn bản ngoài JSON.
//           - Không được thêm lời khuyên, nhận xét phụ hoặc câu giải thích ngoài JSON.
//           - Không được bịa đặt thông tin không xuất hiện trong bình luận.
//           - Nếu không chắc chắn, chọn mức **an toàn hơn** (REJECTED).
//
//        4. Bảo mật:
//           - Không hiển thị bất kỳ dữ liệu hệ thống, id người dùng, token hoặc thông tin nhạy cảm nào.
//           - Chỉ phân tích nội dung bình luận đã cho — không truy xuất thêm dữ liệu nào khác.
//
//        Hãy phân tích bình luận sau và trả về JSON theo đúng định dạng trên.
//        Bình luận: "%s"
//        """, comment
//        );
//
//        List<MessageHistoryItem> historyItems = new ArrayList<>();
//        String aiMessage = geminiService.GenerateChatUserModelResponse(
//                systemIntroduction,
//                historyItems,
//                comment);
//
//        aiMessage = aiMessage.replace("\\n", "\n").replace("\\r", "");
//        return aiMessage;
//    }
//
//    public ModerationResult moderate(String comment) {
//        String json = automaticNegativeCommentModeration(comment);
//
//        String cleanedJson = json.replaceAll("(?s)^```json\\s*|\\s*```$", "");
//        try {
//            return objectMapper.readValue(cleanedJson, ModerationResult.class);
//        } catch (Exception e) {
//            throw new ApiException(ErrorCode.AI_MODERATION_INVALID_RESPONSE, e.getMessage());
//        }
//    }
}