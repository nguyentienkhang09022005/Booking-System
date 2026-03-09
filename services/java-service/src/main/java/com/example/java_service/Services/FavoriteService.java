package com.example.java_service.Services;

import com.example.java_service.Configs.RabbitConfig;
import com.example.java_service.Constant.PredefinedRefFavoriteAndEvaluate;
import com.example.java_service.Dto.Request.FavoriteRequest;
import com.example.java_service.Dto.Response.NotificationCreatedEvent;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.FavoriteMapper;
import com.example.java_service.Models.Accommodation;
import com.example.java_service.Models.Tour;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.AccommodationRepository;
import com.example.java_service.Repositories.Jpa.TourRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import com.example.java_service.Repositories.Mongodb.FavoriteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FavoriteService {

    AccommodationRepository accommodationRepository;
    TourRepository tourRepository;
    UserRepository userRepository;
    FavoriteRepository favoriteRepository;
    FavoriteMapper favoriteMapper;
    ObjectMapper objectMapper;
    RabbitTemplate rabbitTemplate;
    RabbitConfig rabbitConfig;

    // Hàm tạo yêu thích
    public ResponseEntity<String> saveFavorite(FavoriteRequest request) {
        Users user = userRepository.findById(UUID.fromString(request.getIdUser())).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        UUID ownerId = null;
        String targetName = "";
        String targetTypeVi = ""; // Để dùng cho thông báo tiếng Việt

        // Xử lý kiểm tra dựa vào refType
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

        Boolean checkFavorite = favoriteRepository.existsByIdUserAndRefIdAndRefType(
                request.getIdUser(), request.getRefId(), request.getRefType()
        );

        if (checkFavorite) {
            throw new ApiException(ErrorCode.USER_LIKED_ACCOMMODATION);
        }

        var favorite = favoriteMapper.toFavorite(request);
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteRepository.save(favorite);

        String fullName = String.join(" ", user.getFirstName(), user.getLastName());

        // Gửi sự kiện vào Exchanges
        NotificationCreatedEvent event = NotificationCreatedEvent.builder()
                .idNotification(UUID.randomUUID())
                .idUser(ownerId)
                .title("Thông báo yêu thích " + targetTypeVi)
                .content(String.format(
                        "Người dùng %s vừa thêm %s %s vào danh sách yêu thích.",
                        fullName, targetTypeVi, targetName
                ))
                .type("favorite")
                .idRef(favorite.getId())
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
            log.error("Lỗi gửi thông báo RabbitMQ", ex);
        }

        return ResponseEntity.ok("Tạo yêu thích thành công!");
    }

    // Hàm xóa yêu thích
    public ResponseEntity<String> deleteFavorite(FavoriteRequest request) {
        if (!userRepository.existsById(UUID.fromString(request.getIdUser()))) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        // Kiểm tra tồn tại dịch vụ
        if (PredefinedRefFavoriteAndEvaluate.REF_ACCOMMODATION.equalsIgnoreCase(request.getRefType())) {
            if (!accommodationRepository.existsById(UUID.fromString(request.getRefId())))
                throw new ApiException(ErrorCode.ACCOMMODATION_NOT_FOUND);
        } else if (PredefinedRefFavoriteAndEvaluate.REF_TOUR.equalsIgnoreCase(request.getRefType())) {
            if (!tourRepository.existsById(UUID.fromString(request.getRefId())))
                throw new ApiException(ErrorCode.TOUR_NOT_FOUND);
        }

        favoriteRepository.deleteByIdUserAndRefIdAndRefType(
                request.getIdUser(), request.getRefId(), request.getRefType()
        );

        return ResponseEntity.ok("Bỏ yêu thích thành công!");
    }
}