package com.example.Booking_Hotel_BE.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode
{
    // Lỗi xác thực người dùng
    UNAUTHENTICATED("UNAUTHENTICATED", "Unautheticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("UNAUTHORIZED", "Unauthorized", HttpStatus.FORBIDDEN),
    PASSWORD_MISMATCH("PASSWORD_MISMATCH", "Password Mismatch", HttpStatus.BAD_REQUEST),

    // Lỗi người dùng
    USER_NOT_FOUND("USER_NOT_FOUND", "User Not Found", HttpStatus.NOT_FOUND),
    USER_EXISTS("USER_EXISTS", "User Exists", HttpStatus.CONFLICT),

    // Lỗi Role
    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "Role Not Found", HttpStatus.NOT_FOUND),

    // Lỗi Type User
    TYPE_USER_NOT_FOUND("TYPE_USER_NOT_FOUND", "Type User Not Found", HttpStatus.NOT_FOUND),

    // Lỗi OTP
    OTP_NOT_FOUND("OTP_NOT_FOUND", "OTP Not Found", HttpStatus.NOT_FOUND),
    OTP_NOT_EXIST_FOR_EMAIL("OTP_NOT_EXIST_FOR_EMAIL", "OTP not exist for email", HttpStatus.INTERNAL_SERVER_ERROR);


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
