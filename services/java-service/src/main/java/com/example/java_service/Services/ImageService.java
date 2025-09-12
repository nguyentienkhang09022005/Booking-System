package com.example.java_service.Services;

import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Impl.UploadImageFile;
import com.example.java_service.Models.Images;
import com.example.java_service.Repositories.Mongodb.ImagesRepository;
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

    // Hàm sửa ảnh với entity có nhiều ảnh

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
