package com.example.java_service.Impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadImageFileImpl implements UploadImageFile
{
    private final Cloudinary cloudinary;

    @Override
    public String uploadImageForAsync(byte[] bytes, String originalFilename) throws IOException {
        assert originalFilename != null;
        String publicValue = generatePublicValue(originalFilename);
        log.info("publicValue is: {}", publicValue);

        String extension = getFileName(originalFilename)[1];
        log.info("extension is: {}", extension);

        File fileUpload = convertForAsync(bytes, originalFilename);
        log.info("fileUpload is: {}", fileUpload);

        cloudinary.uploader().upload(fileUpload, ObjectUtils.asMap("public_id", publicValue));
        cleanDisk(fileUpload);

        return cloudinary.url().generate(StringUtils.join(publicValue, ".", extension));
    }

    @Override
    public String uploadImage(MultipartFile file) throws IOException {
        assert file.getOriginalFilename() != null;
        String publicValue = generatePublicValue(file.getOriginalFilename());
        log.info("publicValue is: {}", publicValue);
        String extension = getFileName(file.getOriginalFilename())[1];
        log.info("extension is: {}", extension);
        File fileUpload = convert(file);
        log.info("fileUpload is: {}", fileUpload);
        cloudinary.uploader().upload(fileUpload, ObjectUtils.asMap("public_id", publicValue));
        cleanDisk(fileUpload);
        return  cloudinary.url().generate(StringUtils.join(publicValue, ".", extension));
    }

    // Hàm convert cho lưu ảnh Async
    private File convertForAsync(byte[] bytes, String originalFilename) throws IOException {
        String extension = getFileName(originalFilename)[1];
        File convFile = File.createTempFile(UUID.randomUUID().toString(), "." + extension);
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(bytes);
        }
        return convFile;
    }

    // Hàm convert lưu ảnh bình thường
    private File convert(MultipartFile file) throws IOException {
        assert file.getOriginalFilename() != null;
        File convFile = new File(StringUtils.join(generatePublicValue(file.getOriginalFilename()), getFileName(file.getOriginalFilename())[1]));
        try(InputStream is = file.getInputStream()) {
            Files.copy(is, convFile.toPath());
        }
        return convFile;
    }

    private void cleanDisk(File file) {
        try {
            log.info("file.toPath(): {}", file.toPath());
            Path filePath = file.toPath();
            Files.delete(filePath);
        } catch (IOException e) {
            log.error("Error");
        }
    }

    public String generatePublicValue(String originalName){
        String fileName = getFileName(originalName)[0];
        return StringUtils.join(UUID.randomUUID().toString(), "_", fileName);
    }

    public String[] getFileName(String originalName) {
        return originalName.split("\\.");
    }
}
