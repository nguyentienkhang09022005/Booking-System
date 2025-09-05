package com.example.java_service.Exception;

public class ApiException extends RuntimeException
{
    private ErrorCode errorCode;

    public ApiException(ErrorCode errorCode)
    {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
