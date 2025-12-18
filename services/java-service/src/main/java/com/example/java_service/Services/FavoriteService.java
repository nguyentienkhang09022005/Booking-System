package com.example.java_service.Services;

import com.example.java_service.Configs.RabbitConfig;
import com.example.java_service.Dto.Request.FavoriteRequest;
import com.example.java_service.Dto.Response.NotificationCreatedEvent;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.FavoriteMapper;
import com.example.java_service.Models.Favorite;
import com.example.java_service.Models.Hotel;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.HotelRepository;
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
    HotelRepository hotelRepository;
    UserRepository userRepository;
    FavoriteRepository favoriteRepository;
    FavoriteMapper favoriteMapper;
    ObjectMapper objectMapper;
    RabbitTemplate rabbitTemplate;
    RabbitConfig rabbitConfig;

    // Hàm tạo yêu thích khách sạn
    public ResponseEntity<String> saveFavorite(FavoriteRequest request)
    {
        Hotel hotel = hotelRepository.findById(UUID.fromString(request.getIdHotel())).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );
        Users user = userRepository.findById(UUID.fromString(request.getIdUser())).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Boolean checkFavorite = favoriteRepository.existsByIdUserAndIdHotel(request.getIdUser(), request.getIdHotel());
        if (checkFavorite)
            throw new ApiException(ErrorCode.USER_LIKED_HOTEL);

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
                .idUser(hotel.getUsers().getIdUser())
                .title("Thông báo yêu thích khách sạn")
                .content(String.format(
                        "Người dùng %s vừa thêm khách sạn %s vào danh sách yêu thích.",
                        fullName,
                        hotel.getHotelName()
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

    // Hàm xóa yêu thích khách sạn
    public ResponseEntity<String> deleteFavorite(FavoriteRequest request)
    {
        if (!userRepository.existsById(UUID.fromString(request.getIdUser())))
            throw new ApiException(ErrorCode.USER_NOT_FOUND);

        if (!hotelRepository.existsById(UUID.fromString(request.getIdHotel())))
            throw new ApiException(ErrorCode.HOTEL_NOT_FOUND);

        favoriteRepository.deleteByIdUserAndIdHotel(request.getIdUser(), request.getIdHotel());
        return ResponseEntity.ok("Bỏ yêu thích thành công!");
    }
}
