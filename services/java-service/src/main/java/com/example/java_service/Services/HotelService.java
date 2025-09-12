package com.example.java_service.Services;

import com.example.java_service.Dto.Request.HotelCreationRequest;
import com.example.java_service.Dto.Request.HotelUpdateRequest;
import com.example.java_service.Dto.Response.GetListHotelResponse;
import com.example.java_service.Dto.Response.HotelResponse;
import com.example.java_service.Dto.Response.OwnerHotelResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Mappers.HotelMapper;
import com.example.java_service.Mappers.UserMapper;
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
import java.util.UUID;
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
    UserMapper userMapper;

    // Lấy danh sách khách sạn
    public List<GetListHotelResponse> getListHotel(UUID idUser)
    {
        Users user = userRepository.findById(idUser).orElseThrow(
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
    public HotelResponse saveHotel(HotelCreationRequest request)
    {
        Users user = userRepository.findById(request.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Hotel hotel = hotelMapper.toHotel(request);
        hotel.setUsers(user);
        Hotel savedHotel = hotelRepository.save(hotel);

        // Lưu ảnh từ URL đã upload
        if (request.getListImageHotel() != null && !request.getListImageHotel().isEmpty()) {
            List<Images> images = request.getListImageHotel().stream().map(url -> {
                Images image = new Images();
                image.setUrl(url);
                image.setRefId(savedHotel.getIdHotel().toString());
                return image;
            }).toList();
            imagesRepository.saveAll(images);
        }
        OwnerHotelResponse ownerHotelResponse = userMapper.toOwnerHotelResponse(user);
        var hotelResponse = hotelMapper.toHotelCreationResponse(savedHotel);
        hotelResponse.setOwnerHotelResponse(ownerHotelResponse);
        return hotelResponse;
    }

    // Sửa thông tin khách sạn
    public HotelResponse updateHotel(UUID idHotel, HotelUpdateRequest request)
    {
        Hotel hotel = hotelRepository.findById(idHotel).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );

        Users user = userRepository.findById(hotel.getUsers().getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );
        hotelMapper.updateHotelFromRequest(request, hotel);
        Hotel updateHotel = hotelRepository.save(hotel);

        // Nếu có upload ảnh mới
        if (request.getListImageHotel() != null && !request.getListImageHotel().isEmpty()) {
            List<Images> images = request.getListImageHotel().stream().map(url -> {
                Images image = new Images();
                image.setUrl(url);
                image.setRefId(updateHotel.getIdHotel().toString());
                return image;
            }).toList();
            imagesRepository.saveAll(images);
        }
        OwnerHotelResponse ownerHotelResponse = userMapper.toOwnerHotelResponse(user);
        var hotelResponse = hotelMapper.toHotelCreationResponse(updateHotel);
        hotelResponse.setOwnerHotelResponse(ownerHotelResponse);
        return hotelResponse;
    }

    // Xóa khách sạn
    public ResponseEntity<String> deleteHotel(UUID idHotel)
    {
        Hotel hotel = hotelRepository.findById(idHotel).orElseThrow(
                () -> new ApiException(ErrorCode.HOTEL_NOT_FOUND)
        );
        imagesRepository.deleteByRefId(idHotel.toString());
        hotelRepository.delete(hotel);
        return ResponseEntity.ok("Xóa khách sạn thành công!");
    }
}
