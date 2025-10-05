package com.example.Booking_Hotel_BE.Exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException
{
    private ErrorCode errorCode;
    private String message;

    public ApiException(ErrorCode errorCode)
    {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
    }
}
