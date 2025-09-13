package com.example.java_service.Services;

import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Impl.UploadImageFile;
import com.example.java_service.Models.Images;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ImageService
{
    UploadImageFile uploadImageFile;
    ImagesRepository imagesRepository;

    // Hàm thêm ảnh
    public List<String> saveImage(List<MultipartFile> imageFiles)
    {
        return imageFiles.stream().map(file -> {
            try {
                return uploadImageFile.uploadImage(file);
            } catch (Exception e)
            {
                throw new ApiException(ErrorCode.UPLOAD_FAILED);
            }
        }).toList();
    }

    // Hàm sửa ảnh đại diện
    public ResponseEntity<String> updateAvatar(MultipartFile file, String refId)
    {
        imagesRepository.deleteByRefId(refId);
        try {
            return ResponseEntity.ok(uploadImageFile.uploadImage(file));
        } catch (Exception e) {
            throw new ApiException(ErrorCode.UPLOAD_FAILED);
        }
    }

    // Hàm sủa nhiều ảnh (Hotel, Room,...)
    @Transactional
    public ResponseEntity<List<String>> updateImages(List<MultipartFile> files, String refId) {
        if (files == null || files.isEmpty()) {
            throw new ApiException(ErrorCode.IMAGE_FILE_REQUIRED);
        }

        imagesRepository.deleteAllByRefId(refId);

        try {
            // Upload song song để tăng tốc (Java parallel stream)
            List<String> uploadedUrls = files.parallelStream().map(file -> {
                try {
                    return uploadImageFile.uploadImage(file);
                } catch (Exception e) {
                    throw new ApiException(ErrorCode.UPLOAD_FAILED);
                }
            }).toList();

            List<Images> newImages = uploadedUrls.stream().map(url -> {
                Images image = new Images();
                image.setUrl(url);
                image.setRefId(refId);
                return image;
            }).toList();
            imagesRepository.saveAll(newImages);

            return ResponseEntity.ok(uploadedUrls);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.UPLOAD_FAILED);
        }
    }

    // Hàm xóa ảnh
    public ResponseEntity<String> deleteImage(List<String> idImages)
    {
        if (idImages == null || idImages.isEmpty()){
            throw new ApiException(ErrorCode.IMAGE_ID_LIST_EMPTY);
        }
        imagesRepository.deleteAllById(idImages);

        return ResponseEntity.ok("Đã xóa ảnh thành công!");
    }
}
