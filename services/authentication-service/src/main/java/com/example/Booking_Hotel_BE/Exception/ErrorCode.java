package com.example.Booking_Hotel_BE.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode
{
    // Lỗi xác thực người dùng
    UNAUTHENTICATED("UNAUTHENTICATED", "Chưa xác thực", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("UNAUTHORIZED", "Không có quyền truy cập", HttpStatus.FORBIDDEN),
    PASSWORD_MISMATCH("PASSWORD_MISMATCH", "Mật khẩu không khớp", HttpStatus.BAD_REQUEST),

    // Lỗi người dùng
    USER_NOT_FOUND("USER_NOT_FOUND", "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    USER_EXISTS("USER_EXISTS", "Người dùng đã tồn tại", HttpStatus.CONFLICT),
    EMAIL_NOT_FOUND("USER_NOT_EMAIL", "Không tìm thấy email người dùng", HttpStatus.NOT_FOUND),

    // Lỗi Role
    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "Không tìm thấy vai trò", HttpStatus.NOT_FOUND),

    // Lỗi Type User
    TYPE_USER_NOT_FOUND("TYPE_USER_NOT_FOUND", "Không tìm thấy loại người dùng", HttpStatus.NOT_FOUND),

    // Lỗi OTP
    OTP_NOT_FOUND("OTP_NOT_FOUND", "Không tìm thấy OTP", HttpStatus.NOT_FOUND),
    OTP_NOT_EXIST_FOR_EMAIL("OTP_NOT_EXIST_FOR_EMAIL", "Không tồn tại OTP cho email này", HttpStatus.INTERNAL_SERVER_ERROR);

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
