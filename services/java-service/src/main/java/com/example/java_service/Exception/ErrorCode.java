package com.example.java_service.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode
{
    // Lỗi xác thực người dùng
    UNAUTHENTICATED("UNAUTHENTICATED", "Chưa xác thực!", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("UNAUTHORIZED", "Không có quyền truy cập!", HttpStatus.FORBIDDEN),

    // Lỗi người dùng
    USER_NOT_FOUND("USER_NOT_FOUND", "Không tìm thấy người dùng!", HttpStatus.NOT_FOUND),
    USER_EXISTS("USER_EXISTS", "Người dùng đã tồn tại!", HttpStatus.CONFLICT),
    EMAIL_EXISTS("EMAIL_EXISTS", "Email đã tồn tại!", HttpStatus.CONFLICT),
    PHONE_EXISTS("PHONE_EXISTS", "Số điện thoại dùng đã tồn tại!", HttpStatus.CONFLICT),
    INVALID_PHONE_LENGTH("INVALID_PHONE_LENGTH", "Số điện thoại phải từ 8 đến 12 số! ", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_FORMAT("INVALID_PHONE_FORMAT", "Số điện thoại chỉ chứa số!", HttpStatus.BAD_REQUEST),
    PHONE_REQUIRED("PHONE_REQUIRED", "Vui lòng nhập số đện thoại", HttpStatus.BAD_REQUEST),

    // Lỗi Role
    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "Không tìm thấy vai trò!", HttpStatus.NOT_FOUND),
    ROLE_EXISTS("ROLE_EXISTS", "Quyền đã tồn tại!", HttpStatus.CONFLICT),

    // Lỗi Type User
    TYPE_USER_NOT_FOUND("TYPE_USER_NOT_FOUND", "Không tìm thấy loại người dùng!", HttpStatus.NOT_FOUND),
    TYPE_USER_EXISTS("TYPE_USER_EXISTS", "Loại người dùng đã tồn tại!", HttpStatus.CONFLICT),

    // Lỗi Type Room
    TYPE_ROOM_NOT_FOUND("TYPE_ROOM_NOT_FOUND", "Không tìm thấy loại phòng!", HttpStatus.NOT_FOUND),
    TYPE_ROOM_EXISTS("TYPE_ROOM_EXISTS", "Loại phòng đã tồn tại!", HttpStatus.CONFLICT),

    // Lỗi Type Service
    TYPE_SERVICE_NOT_FOUND("TYPE_SERVICE_NOT_FOUND", "Không tìm thấy loại dịch vụ!", HttpStatus.NOT_FOUND),
    TYPE_SERVICE_EXISTS("TYPE_SERVICE_EXISTS", "Loại dịch vụ đã tồn tại!", HttpStatus.CONFLICT),

    // Lỗi lưu hình ảnh
    IMAGE_ID_LIST_EMPTY("IMAGE_ID_LIST_EMPTY", "Danh sách id hình ảnh không được để trống!", HttpStatus.BAD_REQUEST),
    UPLOAD_FAILED("UPLOAD_FAILED", "tải ảnh thất bại!", HttpStatus.INTERNAL_SERVER_ERROR),
    IMAGE_FILE_REQUIRED("IMAGE_FILE_REQUIRED", "Vui lòng nhập hình ảnh!", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_REFID("INVALID_IMAGE_REFID", "Thiếu id đối tượng sở hữu hình ảnh", HttpStatus.BAD_REQUEST),

    // Lỗi khách sạn
    HOTEL_NOT_FOUND("HOTEL_NOT_FOUND", "Không tìm thấy khách sạn!", HttpStatus.NOT_FOUND),

    // Lỗi phòng
    ROOM_NOT_FOUND("ROOM_NOT_FOUND", "Không tìm thấy phòng!", HttpStatus.NOT_FOUND),

    // Dịch vụ
    SERVICE_NOT_FOUND("SERVICE_NOT_FOUND", "Không tìm thấy dịch vụ!", HttpStatus.NOT_FOUND),
    ;

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
