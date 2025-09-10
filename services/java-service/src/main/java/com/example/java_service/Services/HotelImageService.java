package com.example.java_service.Services;

import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Impl.ImageUploadData;
import com.example.java_service.Impl.UploadImageFile;
import com.example.java_service.Models.Hotel;
import com.example.java_service.Models.Images;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HotelImageService
{
    private final ImagesRepository imagesRepository;
    private final UploadImageFile uploadImageFile;

    @Async
    public void uploadImageHotelAsync(List<MultipartFile> files, Hotel hotel)
    {
        List<ImageUploadData> imageUploadDataList;
        try {
            // Đọc toàn bộ file thành byte array
            imageUploadDataList = files.stream()
                    .map(file -> {
                        try {
                            return new ImageUploadData(file.getBytes(), file.getOriginalFilename());
                        } catch (IOException e) {
                            throw new RuntimeException("Không thể đọc được file: " + file.getOriginalFilename(), e);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return; // Dừng lại nếu có lỗi khi đọc file
        }
        processAndSaveImages(imageUploadDataList, hotel);
    }

    @Async
    public void processAndSaveImages(List<ImageUploadData> imageUploadDataList, Hotel hotel) {
        try {
            List<String> urls = imageUploadDataList.stream().map(data -> {
                try {
                    // Sử dụng dữ liệu từ ImageUploadData
                    return uploadImageFile.uploadImageForAsync(data.content(), data.originalFilename());
                } catch (Exception e) {
                    throw new ApiException(ErrorCode.UPLOAD_FAILED);
                }
            }).collect(Collectors.toList());

            List<Images> imagesList = urls.stream().map(url -> {
                Images image = new Images();
                image.setUrl(url);
                image.setRefId(hotel.getIdHotel().toString());
                image.setUploadedAt(LocalDateTime.now());
                return image;
            }).collect(Collectors.toList());

            imagesRepository.saveAll(imagesList);

        } catch (Exception e) {
            log.error("Hệ thống xữ lý lưu ảnh cho khách sạn thất bại: {}. Error: {}", hotel.getIdHotel(), e.getMessage());
        }
    }
}
