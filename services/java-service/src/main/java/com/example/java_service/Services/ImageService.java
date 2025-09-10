package com.example.java_service.Services;

import com.example.java_service.Exception.ApiException;
import com.example.java_service.Exception.ErrorCode;
import com.example.java_service.Impl.UploadImageFile;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
}
