package com.example.java_service.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode
{
    // Lỗi xác thực người dùng
    UNAUTHENTICATED("UNAUTHENTICATED", "Chưa xác thực", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("UNAUTHORIZED", "Không có quyền truy cập", HttpStatus.FORBIDDEN),

    // Lỗi người dùng
    USER_NOT_FOUND("USER_NOT_FOUND", "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),

    // Lỗi lưu hình ảnh
    UPLOAD_FAILED("UPLOAD_FAILED", "tải ảnh thất bại", HttpStatus.INTERNAL_SERVER_ERROR),

    // Lỗi khách sạn
    HOTEL_NOT_FOUND("HOTEL_NOT_FOUND", "Không tìm thấy khách sạn", HttpStatus.NOT_FOUND),;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus)
    {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;

    }
}
