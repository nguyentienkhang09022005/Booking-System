package com.example.java_service.Services;

import com.example.java_service.Constant.TypeImageConstant;
import com.example.java_service.Dto.Request.HotelCreationRequest;
import com.example.java_service.Dto.Response.HotelCreationResponse;
import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Impl.UploadImageFile;
import com.example.java_service.Mappers.HotelMapper;
import com.example.java_service.Models.Hotel;
import com.example.java_service.Models.Images;
import com.example.java_service.Models.Users;
import com.example.java_service.Repositories.Jpa.HotelRepository;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
import com.example.java_service.Repositories.Jpa.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HotelService
{
    UserRepository userRepository;
    HotelRepository hotelRepository;
    ImagesRepository imagesRepository;
    HotelMapper hotelMapper;
    UploadImageFile uploadImageFile;

    // Lấy danh sách khách sạn theo paging 8
    // Lấy thông tin chi tiết của khách sạn qua id
    // Thêm khách sạn mới

    @Transactional
    public HotelCreationResponse saveHotel(HotelCreationRequest request)
    {
        Users user = userRepository.findById(request.getIdUser()).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND)
        );

        Hotel hotel = hotelMapper.toHotel(request);
        hotel.setUsers(user);
        Hotel savedHotel = hotelRepository.save(hotel);

        List<Images> imagesListToSave = new ArrayList<>();
        List<String> imageHotelUrls = new ArrayList<>();

        List<MultipartFile> files = request.getListImageHotel();
        if (request.getListImageHotel() != null && !request.getListImageHotel().isEmpty())
        {
            imageHotelUrls = files.parallelStream().map(file -> {
                try {
                    return uploadImageFile.uploadImage(file);
                }catch (Exception e) {
                    throw new ApiException(ErrorCode.UPLOAD_FAILED);
                }
            }).collect(Collectors.toList());
            imagesListToSave = imageHotelUrls.stream().map(url -> {
                Images image = new Images();
                image.setUrl(url);
                image.setType(TypeImageConstant.HOTEL_TYPE);
                image.setRefId(savedHotel.getIdHotel().toString());
                image.setUploadedAt(LocalDateTime.now());
                return image;
            }).collect(Collectors.toList());

            if (!imagesListToSave.isEmpty()) {
                imagesRepository.saveAll(imagesListToSave);
            }
        }
        HotelCreationResponse hotelCreationResponse = hotelMapper.toHotelCreationResponse(hotel);
        hotelCreationResponse.setUsers(user);
        hotelCreationResponse.setImageHotelUrl(imageHotelUrls);
        return hotelCreationResponse;
    }

    // Sửa thông tin khách sạn
    // Xóa khách sạn
}
