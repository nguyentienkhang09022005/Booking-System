package com.example.java_service.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode
{
    // Lỗi chung
    QUANTITY_NOT_ENOUGH("QUANTITY_NOT_ENOUGH", "Số lượng không được nhỏ hơn 0", HttpStatus.BAD_REQUEST),
    PERCENT_NOT_NEGATIVE("PERCENT_NOT_NEGATIVE", "Phần trăm không được nhỏ hơn 0", HttpStatus.BAD_REQUEST),

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
    PHONE_REQUIRED("PHONE_REQUIRED", "Vui lòng nhập số đện thoại!", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL_FORMAT("INVALID_EMAIL_FORMAT", "Lỗi format email (example@gmail.com)!", HttpStatus.BAD_REQUEST),
    EMAIL_REQUIRED("EMAIL_REQUIRED", "Vui lòng nhập email!", HttpStatus.BAD_REQUEST),

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
    USER_CREATED_HOTEL("USER_CREATED_HOTEL", "Tài khoản này đã tạo khách sạn rồi!", HttpStatus.CONFLICT),

    // Lỗi phòng
    ROOM_NOT_FOUND("ROOM_NOT_FOUND", "Không tìm thấy phòng!", HttpStatus.NOT_FOUND),
    ROOM_ALREADY_BOOKED("ROOM_ALREADY_BOOKED", "Phòng đã được đặt!", HttpStatus.CONFLICT),
    ROOM_NOT_ENOUGH_QUANTITY("ROOM_NOT_ENOUGH_QUANTITY", "Không đủ số lượng phòng!", HttpStatus.BAD_REQUEST),

    // Lỗi dịch vụ
    SERVICE_NOT_FOUND("SERVICE_NOT_FOUND", "Không tìm thấy dịch vụ!", HttpStatus.NOT_FOUND),
    SERVICE_NOT_ENOUGH_QUANTITY("SERVICE_NOT_ENOUGH_QUANTITY", "Không đủ số lượng dịch vụ!", HttpStatus.BAD_REQUEST),

    // Lỗi yêu thích
    FAVORITE_NOT_FOUND("FAVORITE_NOT_FOUND", "Lỗi không tìm thấy khách sạn yêu thích!", HttpStatus.NOT_FOUND),
    USER_LIKED_HOTEL("USER_LIKED_HOTEL", "Bạn đã yêu thích khách sạn này rồi!", HttpStatus.CONFLICT),

    // Lỗi đánh giá
    EVALUATE_NOT_FOUND("EVALUATE_NOT_FOUND", "Lỗi không tìm thấy đánh giá!", HttpStatus.NOT_FOUND),
    USER_EVALUATED_HOTEL("USER_EVALUATED_HOTEL", "Bạn đã đánh giá khách sạn này rồi!", HttpStatus.CONFLICT),
    EVALUATE_UPDATE_TIME_EXPIRED("EVALUATE_UPDATE_TIME_EXPIRED", "Chỉ được thay đổi đánh giá trong 30 phút!", HttpStatus.BAD_REQUEST),

    // Lỗi đặt phòng
    BOOKING_NOT_FOUND("BOOKING_NOT_FOUND", "Lỗi xữ lý đặt phòng!", HttpStatus.NOT_FOUND),
    BOOKING_FAILED("BOOKING_FAILED", "Đặt phòng thất bại!", HttpStatus.INTERNAL_SERVER_ERROR),
    BOOKING_NOT_CANCEL("BOOKING_NOT_CANCEL", "Không thể hủy đặt phòng!", HttpStatus.BAD_REQUEST),
    CHECKOUT_DATE_BEFORE_CHECKIN_DATE("CHECKOUT_DATE_BEFORE_CHECKIN_DATE", "Lỗi: Ngày checkout không được trước ngày checkin!", HttpStatus.BAD_REQUEST),

    // Lỗi thanh toán
    PAYMENT_NOT_FOUND("PAYMENT_NOT_FOUND", "Không tìm thấy thông tin thanh toán!", HttpStatus.NOT_FOUND),
    PAYMENT_FAILED("PAYMENT_FAILED", "Thanh toán thất bại!", HttpStatus.INTERNAL_SERVER_ERROR),
    PAID("PAYMENTED", "Đã thanh toán không thể thay đổi thông tin!", HttpStatus.BAD_REQUEST),
    USER_UNPAID("USER_UNPAID", "Người dùng chưa thanh toán không thể thay đổi thông tin!", HttpStatus.BAD_REQUEST),

    // Lỗi voucher
    CODE_EXISTED("CODE_EXISTED", "Mã voucher đã tồn tại!", HttpStatus.CONFLICT),
    END_DATE_BEFORE_STAR_DATE("END_DATE_BEFORE_STAR_DATE", "Ngày kết thúc không được trước ngày bắt đầu", HttpStatus.BAD_REQUEST),
    VOUCHER_NOT_FOUND("VOUCHER_NOT_FOUND", "Không tìm thấy voucher!", HttpStatus.NOT_FOUND),
    CODE_REQUIRED("CODE_REQUIRED", "Vui lòng nhập code!", HttpStatus.BAD_REQUEST),
    VOUCHER_NOT_ENOUGH_QUANTITY("VOUCHER_NOT_ENOUGH_QUANTITY", "Không đủ số lượng voucher!", HttpStatus.BAD_REQUEST),

    // Lỗi UserVoucher
    VOUCHER_ALREADY_SAVED("VOUCHER_ALREADY_SAVED", "Voucher đã được lưu!", HttpStatus.CONFLICT),


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
