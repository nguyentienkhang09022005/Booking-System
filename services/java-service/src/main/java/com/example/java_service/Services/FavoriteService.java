package com.example.java_service.Services;

import com.example.java_service.Configs.RabbitConfig;
import com.example.java_service.Dto.Request.FavoriteRequest;
import com.example.java_service.Dto.Response.NotificationCreatedEvent;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.FavoriteMapper;
import com.example.java_service.Models.Accommodation;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.AccommodationRepository;
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
public class FavoriteService
{
    AccommodationRepository accommodationRepository;
    UserRepository userRepository;
    FavoriteRepository favoriteRepository;
    FavoriteMapper favoriteMapper;
    ObjectMapper objectMapper;
    RabbitTemplate rabbitTemplate;
    RabbitConfig rabbitConfig;

    // Hàm tạo yêu thích nơi cư trú
    public ResponseEntity<String> saveFavorite(FavoriteRequest request)
    {
        Accommodation accommodation = accommodationRepository.findById(UUID.fromString(request.getIdAccommodation())).orElseThrow(
                () -> new ApiException(ErrorCode.ACCOMMODATION_NOT_FOUND)
        );
        Users user = userRepository.findById(UUID.fromString(request.getIdUser())).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Boolean checkFavorite = favoriteRepository.existsByIdUserAndIdAccommodation(request.getIdUser(), request.getIdAccommodation());
        if (checkFavorite)
            throw new ApiException(ErrorCode.USER_LIKED_ACCOMMODATION);

        var favorite = favoriteMapper.toFavorite(request);
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteRepository.save(favorite);

        String fullName = String.join(" ",
                user.getFirstName(),
                user.getLastName()
        );
        // Gửi sự kiện vào Exchanges
        NotificationCreatedEvent event = NotificationCreatedEvent.builder()
                .idNotification(UUID.randomUUID())
                .idUser(accommodation.getUsers().getIdUser())
                .title("Thông báo yêu thích nơi cư trú")
                .content(String.format(
                        "Người dùng %s vừa thêm nơi cư trú %s vào danh sách yêu thích.",
                        fullName,
                        accommodation.getAccommodationName()
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
            ex.printStackTrace();
        }
        return ResponseEntity.ok("Tạo yêu thích thành công!");
    }

    // Hàm xóa yêu thích nơi cư trú
    public ResponseEntity<String> deleteFavorite(FavoriteRequest request)
    {
        if (!userRepository.existsById(UUID.fromString(request.getIdUser())))
            throw new ApiException(ErrorCode.USER_NOT_FOUND);

        if (!accommodationRepository.existsById(UUID.fromString(request.getIdAccommodation())))
            throw new ApiException(ErrorCode.ACCOMMODATION_NOT_FOUND);

        favoriteRepository.deleteByIdUserAndIdAccommodation(request.getIdUser(), request.getIdAccommodation());
        return ResponseEntity.ok("Bỏ yêu thích thành công!");
    }
}
