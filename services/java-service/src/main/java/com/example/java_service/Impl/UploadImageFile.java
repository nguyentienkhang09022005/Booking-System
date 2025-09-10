package com.example.java_service.Impl;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface UploadImageFile
{
    String uploadImageForAsync(byte[] bytes, String originalFilename) throws IOException;
    String uploadImage(MultipartFile file) throws IOException;
}
