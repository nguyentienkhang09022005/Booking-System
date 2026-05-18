package com.example.java_service.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode
{
    // Lỗi chung
    QUANTITY_NOT_ENOUGH("QUANTITY_NOT_ENOUGH", "Số lượng không được nhỏ hơn 0", HttpStatus.BAD_REQUEST),
    PERCENT_NOT_NEGATIVE("PERCENT_NOT_NEGATIVE", "Phần trăm không được nhỏ hơn 0", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("INTERNAL_ERROR", "Lỗi phía máy chủ", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_MODERATION_INVALID_RESPONSE("AI_MODERATION_INVALID_RESPONSE", "Mô hình AI đang gặp sự cố, không thể kiểm duệt bình luận", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_MODERATION_SERVICE_UNAVAILABLE("AI_MODERATION_SERVICE_UNAVAILABLE", "Dịch vụ kiểm duyệt AI tạm thời không khả dụng", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_DATA("INVALID_DATA", "Dữ liệu không hợp lệ!", HttpStatus.BAD_REQUEST),

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
    ACCOMMODATION_NOT_FOUND("ACCOMMODATION_NOT_FOUND", "Không tìm thấy nơi cư trú!", HttpStatus.NOT_FOUND),
    USER_CREATED_ACCOMMODATION("USER_CREATED_ACCOMMODATION", "Tài khoản này đã tạo nơi cư trú rồi!", HttpStatus.CONFLICT),

    // Lỗi phòng
    ROOM_NOT_FOUND("ROOM_NOT_FOUND", "Không tìm thấy phòng!", HttpStatus.NOT_FOUND),
    ROOM_ALREADY_BOOKED("ROOM_ALREADY_BOOKED", "Phòng đã được đặt!", HttpStatus.CONFLICT),
    ROOM_NOT_ENOUGH_QUANTITY("ROOM_NOT_ENOUGH_QUANTITY", "Không đủ số lượng phòng!", HttpStatus.BAD_REQUEST),

    // Lỗi dịch vụ
    SERVICE_NOT_FOUND("SERVICE_NOT_FOUND", "Không tìm thấy dịch vụ!", HttpStatus.NOT_FOUND),
    SERVICE_NOT_ENOUGH_QUANTITY("SERVICE_NOT_ENOUGH_QUANTITY", "Không đủ số lượng dịch vụ!", HttpStatus.BAD_REQUEST),

    // Lỗi yêu thích
    FAVORITE_NOT_FOUND("FAVORITE_NOT_FOUND", "Lỗi không tìm thấy nơi cư trú yêu thích!", HttpStatus.NOT_FOUND),
    USER_LIKED_ACCOMMODATION("USER_LIKED_ACCOMMODATION", "Bạn đã yêu thích nơi cư trú này rồi!", HttpStatus.CONFLICT),

    // Lỗi đánh giá
    EVALUATE_NOT_FOUND("EVALUATE_NOT_FOUND", "Lỗi không tìm thấy đánh giá!", HttpStatus.NOT_FOUND),
    USER_EVALUATED_ACCOMMODATION("USER_EVALUATED_ACCOMMODATION", "Bạn đã đánh giá nơi cư trú này rồi!", HttpStatus.CONFLICT),
    EVALUATE_UPDATE_TIME_EXPIRED("EVALUATE_UPDATE_TIME_EXPIRED", "Chỉ được thay đổi đánh giá trong 30 phút!", HttpStatus.BAD_REQUEST),
    COMMENT_REJECTED_BY_AI("COMMENT_REJECTED_BY_AI", "Bình luận có phát ngôn không chuẩn mực, đã bị loại bỏ", HttpStatus.BAD_REQUEST),

    // Lỗi đặt phòng
    BOOKING_NOT_FOUND("BOOKING_NOT_FOUND", "Lỗi xữ lý đặt phòng!", HttpStatus.NOT_FOUND),
    BOOKING_FAILED("BOOKING_FAILED", "Tạo đơn thất bại!", HttpStatus.INTERNAL_SERVER_ERROR),
    BOOKING_NOT_CANCEL("BOOKING_NOT_CANCEL", "Không thể hủy đặt phòng!", HttpStatus.BAD_REQUEST),
    CHECKOUT_DATE_BEFORE_CHECKIN_DATE("CHECKOUT_DATE_BEFORE_CHECKIN_DATE", "Lỗi: Thời gian checkout không được trước hoặc bằng thời gian checkin!", HttpStatus.BAD_REQUEST),

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
    VOUCHER_EXPIRED("VOUCHER_EXPIRED", "Voucher đã hết hạn!", HttpStatus.BAD_REQUEST),
    VOUCHER_NOT_BELONG_TO_ACCOMMODATION("VOUCHER_NOT_BELONG_TO_ACCOMMODATION", "Voucher không thuộc về nơi cư trú!", HttpStatus.BAD_REQUEST),
    INVALID_DISCOUNT_AMOUNT("INVALID_DISCOUNT_AMOUNT", "Số tiền giảm tối đa phải lớn hơn hoặc bằng số tiền giảm tối thiểu!", HttpStatus.BAD_REQUEST),
    DISCOUNT_AMOUNT_NOT_NEGATIVE("DISCOUNT_AMOUNT_NOT_NEGATIVE", "Số tiền giảm giá không được nhỏ hơn 0!", HttpStatus.BAD_REQUEST),
    SYSTEM_VOUCHER_MODIFICATION_DENIED("SYSTEM_VOUCHER_MODIFICATION_DENIED", "Không thể chỉnh sửa voucher của hệ thống thông qua API này!", HttpStatus.BAD_REQUEST),
    BUSINESS_VOUCHER_MODIFICATION_DENIED("BUSINESS_VOUCHER_MODIFICATION_DENIED", "Không thể chỉnh sửa voucher của doanh nghiệp thông qua API này!", HttpStatus.BAD_REQUEST),
    VOUCHER_NOT_APPLICABLE("VOUCHER_NOT_APPLICABLE", "Đơn hàng không có hạng mục phù hợp để áp dụng voucher này!", HttpStatus.BAD_REQUEST),
    VOUCHER_NOT_BELONG_TO_TOUR("VOUCHER_NOT_BELONG_TO_TOUR", "Voucher không thuộc về tour này!", HttpStatus.BAD_REQUEST),
    VOUCHER_NOT_BELONG_TO_TRANSPORT("VOUCHER_NOT_BELONG_TO_TRANSPORT", "Voucher không thuộc về vé di chuyển này!", HttpStatus.BAD_REQUEST),

    // Lỗi UserVoucher
    VOUCHER_ALREADY_SAVED("VOUCHER_ALREADY_SAVED", "Voucher đã được lưu!", HttpStatus.CONFLICT),
    VOUCHER_ALREADY_USED("VOUCHER_ALREADY_USED", "Voucher đã được sử dụng", HttpStatus.CONFLICT),

    // Lỗi Chi tiêu
    EXPENSE_NOT_FOUND("EXPENSE_NOT_FOUND", "Không tìm thấy chi tiêu!", HttpStatus.NOT_FOUND),
    INVALID_AMOUNT("INVALID_AMOUNT", "Số tiền không được âm!", HttpStatus.BAD_REQUEST),

    // Lỗi Báo cáo Doanh thu
    REVENUE_REPORT_FAILED("REVENUE_REPORT_FAILED", "Gọi API báo cáo doanh thu thất bại!", HttpStatus.INTERNAL_SERVER_ERROR),

    // Lỗi báo cáo sự cố
    INCIDENT_REPORT_NOT_FOUND("INCIDENT_REPORT_NOT_FOUND", "Không tìm thấy báo cáo sự cố!", HttpStatus.NOT_FOUND),
    INVALID_INCIDENT_REPORT("INVALID_INCIDENT_REPORT", "Báo cáo sự cố đã được xữ lý không thể thay đổi thông tin!", HttpStatus.BAD_REQUEST),
    INVALID_STATUS_INCIDENT_REPORT("INVALID_STATUS_INCIDENT_REPORT", "Trạng thái phải là: Pending, In Progress, Cancelled, Resolved!", HttpStatus.BAD_REQUEST),
    INVALID_SEVERITY_INCIDENT_REPORT("INVALID_SEVERITY_INCIDENT_REPORT", "Mức độ phải là: High, Low, Medium!", HttpStatus.BAD_REQUEST),

    // Lỗi Tour
    TOUR_NOT_FOUND("TOUR_NOT_FOUND", "Không tìm thấy tour du lịch!", HttpStatus.NOT_FOUND),
    INVALID_STATUS_TOUR("INVALID_STATUS_TOUR", "Trạng thái tour không hợp lệ (Chỉ nhận Available hoặc Sold Out)!", HttpStatus.BAD_REQUEST),
    INVALID_DURATION_DAYS("INVALID_DURATION_DAYS", "Số ngày của tour phải lớn hơn 0!", HttpStatus.BAD_REQUEST),
    INVALID_ADULT_PRICE("INVALID_ADULT_PRICE", "Giá vé người lớn không được nhỏ hơn 0!", HttpStatus.BAD_REQUEST),
    INVALID_CHILD_PRICE("INVALID_CHILD_PRICE", "Giá vé trẻ em không được nhỏ hơn 0!", HttpStatus.BAD_REQUEST),

    // Lỗi Transport Ticket
    TRANSPORT_TICKET_NOT_FOUND("TRANSPORT_TICKET_NOT_FOUND", "Không tìm thấy vé di chuyển!", HttpStatus.NOT_FOUND),
    INVALID_TICKET_TYPE("INVALID_TICKET_TYPE", "Loại vé không hợp lệ (Chỉ nhận Coach, Plane, Train, PassengerShip)!", HttpStatus.BAD_REQUEST),
    INVALID_TICKET_TIME("INVALID_TICKET_TIME", "Thời gian đến (Arrival Time) phải sau thời gian đi (Departure Time)!", HttpStatus.BAD_REQUEST),
    INVALID_TICKET_PRICE("INVALID_TICKET_PRICE", "Giá vé không được âm!", HttpStatus.BAD_REQUEST),
    INVALID_STATUS_TICKET("INVALID_STATUS_TICKET", "Trạng thái Ticket không hợp lệ (Chỉ nhận Available hoặc Sold Out)!", HttpStatus.BAD_REQUEST),

    // Lỗi Booking Tour
    INVALID_STATUS_BOOKING_TOUR("INVALID_STATUS_BOOKING_TOUR", "Trạng thái đặt tour không hợp lệ (Chỉ nhận Pending, Success hoặc Failed)!", HttpStatus.BAD_REQUEST),

    // Lỗi Loyalty Transaction
    LOYALTY_TRANSACTION_NOT_FOUND("LOYALTY_TRANSACTION_NOT_FOUND", "Không tìm thấy giao dịch tích điểm!", HttpStatus.NOT_FOUND),

    // Lỗi User Preferences
    USER_PREFERENCES_NOT_FOUND("USER_PREFERENCES_NOT_FOUND", "Không tìm thấy sở thích người dùng!", HttpStatus.NOT_FOUND),
    USER_PREFERENCES_ALREADY_EXISTS("USER_PREFERENCES_ALREADY_EXISTS", "Sở thích người dùng đã tồn tại!", HttpStatus.CONFLICT),

    // Lỗi Itinerary
    ITINERARY_NOT_FOUND("ITINERARY_NOT_FOUND", "Không tìm thấy lịch trình!", HttpStatus.NOT_FOUND),
    INVALID_ITINERARY_DATE("INVALID_ITINERARY_DATE", "Ngày kết thúc phải sau ngày bắt đầu!", HttpStatus.BAD_REQUEST),
    ITINERARY_CANNOT_GENERATE("ITINERARY_CANNOT_GENERATE", "Không thể tạo lịch trình AI!", HttpStatus.BAD_REQUEST),
    ITINERARY_NOT_IN_DRAFT_STATUS("ITINERARY_NOT_IN_DRAFT_STATUS", "Chỉ có thể tạo lịch trình từ trạng thái DRAFT!", HttpStatus.BAD_REQUEST),

    // Lỗi Itinerary Item
    ITINERARY_ITEM_NOT_FOUND("ITINERARY_ITEM_NOT_FOUND", "Không tìm thấy hạng mục lịch trình!", HttpStatus.NOT_FOUND),
    INVALID_ITINERARY_ITEM_STATUS("INVALID_ITINERARY_ITEM_STATUS", "Trạng thái hạng mục không hợp lệ!", HttpStatus.BAD_REQUEST),
    INVALID_ITEM_TYPE("INVALID_ITEM_TYPE", "Loại hạng mục không hợp lệ!", HttpStatus.BAD_REQUEST),
    INVALID_ITEM_DATE("INVALID_ITEM_DATE", "Ngày của hạng mục không nằm trong khoảng lịch trình!", HttpStatus.BAD_REQUEST),
    INVALID_DISPLAY_ORDER("INVALID_DISPLAY_ORDER", "Thứ tự hiển thị không hợp lệ!", HttpStatus.BAD_REQUEST),

    // Lỗi Bulletin Board
    BULLETIN_NOT_FOUND("BULLETIN_NOT_FOUND", "Không tìm thấy bản tin!", HttpStatus.NOT_FOUND),
    BULLETIN_ACCESS_DENIED("BULLETIN_ACCESS_DENIED", "Bạn không có quyền thực hiện thao tác này!", HttpStatus.FORBIDDEN),
    BULLETIN_INVALID_TARGET("BULLETIN_INVALID_TARGET", "Target audience không hợp lệ!", HttpStatus.BAD_REQUEST),
    BULLETIN_ALREADY_APPROVED("BULLETIN_ALREADY_APPROVED", "Bản tin đã được phê duyệt!", HttpStatus.BAD_REQUEST),
    BULLETIN_ALREADY_REJECTED("BULLETIN_ALREADY_REJECTED", "Bản tin đã bị từ chối!", HttpStatus.BAD_REQUEST),
    BULLETIN_TITLE_REQUIRED("BULLETIN_TITLE_REQUIRED", "Tiêu đề bản tin không được để trống!", HttpStatus.BAD_REQUEST),
    BULLETIN_INVALID_STATUS("BULLETIN_INVALID_STATUS", "Trạng thái bản tin không hợp lệ!", HttpStatus.BAD_REQUEST),

    // Lỗi Personal Notebook
    NOTEBOOK_NOT_FOUND("NOTEBOOK_NOT_FOUND", "Không tìm thấy sổ tay!", HttpStatus.NOT_FOUND),
    NOTEBOOK_ACCESS_DENIED("NOTEBOOK_ACCESS_DENIED", "Bạn không có quyền truy cập sổ tay này!", HttpStatus.FORBIDDEN),
    NOTEBOOK_TITLE_REQUIRED("NOTEBOOK_TITLE_REQUIRED", "Tiêu đề sổ tay không được để trống!", HttpStatus.BAD_REQUEST),
    NOTEBOOK_INVALID_TEMPLATE("NOTEBOOK_INVALID_TEMPLATE", "Template sổ tay không hợp lệ!", HttpStatus.BAD_REQUEST),
    NOTE_NOT_FOUND("NOTE_NOT_FOUND", "Không tìm thấy trang!", HttpStatus.NOT_FOUND),
    NOTE_TITLE_REQUIRED("NOTE_TITLE_REQUIRED", "Tiêu đề trang không được để trống!", HttpStatus.BAD_REQUEST),
    BLOCK_NOT_FOUND("BLOCK_NOT_FOUND", "Không tìm thấy block!", HttpStatus.NOT_FOUND),
    BLOCK_INVALID_TYPE("BLOCK_INVALID_TYPE", "Loại block không hợp lệ!", HttpStatus.BAD_REQUEST)
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
