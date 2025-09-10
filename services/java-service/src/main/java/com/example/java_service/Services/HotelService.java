package com.example.java_service.Services;

import com.example.java_service.Dto.Request.GetListHotelRequest;
import com.example.java_service.Dto.Request.HotelCreationRequest;
import com.example.java_service.Dto.Response.GetListHotelResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.HotelMapper;
import com.example.java_service.Models.Favorite;
import com.example.java_service.Models.Hotel;
import com.example.java_service.Models.Images;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.HotelRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import com.example.java_service.Repositories.Mongodb.FavoriteRepository;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HotelService
{
    UserRepository userRepository;
    HotelRepository hotelRepository;
    FavoriteRepository favoriteRepository;
    ImagesRepository imagesRepository;
    HotelMapper hotelMapper;
    HotelImageService hotelImageService;

    // Lấy danh sách khách sạn
    public List<GetListHotelResponse> getListHotel(GetListHotelRequest request)
    {
        Users user = userRepository.findById(request.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND));

        List<Hotel> hotels = hotelRepository.findAll(); // lấy danh sách hotel

        Set<String> favoriteHotelId = favoriteRepository.findByIdUser(user.getIdUser().toString()) // Lấy danh sách hotel yêu thích
                .stream()
                .map(Favorite::getId)
                .collect(Collectors.toSet());

        return hotels.stream()
                .map(hotel -> {
                    GetListHotelResponse response = hotelMapper.toGetListHotelResponse(hotel);

                    // Set favorite
                    response.setFavorite(favoriteHotelId.contains(hotel.getIdHotel().toString()));

                    // Set image đầu tiên của hotel
                    String firstImageUrl = imagesRepository
                            .findByRefIdOrderByUploadedAtAsc(hotel.getIdHotel().toString())
                            .stream()
                            .findFirst()
                            .map(Images::getUrl)
                            .orElse(null);
                    response.setImageHotelUrl(firstImageUrl);

                    return response;
                }).toList();
    }

    // Lấy thông tin chi tiết của khách sạn qua id

    // Thêm khách sạn mới
    public ResponseEntity<String> saveHotel(HotelCreationRequest request)
    {
        Users user = userRepository.findById(request.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Hotel hotel = hotelMapper.toHotel(request);
        hotel.setUsers(user);
        Hotel savedHotel = hotelRepository.save(hotel);

        if (request.getListImageHotel() != null && !request.getListImageHotel().isEmpty()) {
            hotelImageService.uploadImageHotelAsync(request.getListImageHotel(), savedHotel);
        }
        return ResponseEntity.ok("Tạo khách sạn thành công!");
    }

    // Sửa thông tin khách sạn
    // Xóa khách sạn
}
