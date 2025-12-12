package Errors

type ErrorCode struct {
	Code    int
	Message string
}

var (
	// Server
	InvalidInput  = ErrorCode{Code: 400, Message: "Dữ liệu gửi lên không hợp lệ!"}
	InternalError = ErrorCode{Code: 500, Message: "Lỗi hệ thống!"}

	// User
	UserNotFound = ErrorCode{Code: 404, Message: "Không tìm thấy người dùng!"}

	// Hotel
	HotelNotFound = ErrorCode{Code: 404, Message: "Không tìm thấy khách sạn!"}

	// Report Revenue
	RevenueExists         = ErrorCode{Code: 409, Message: "Báo cáo đã tồn tại trong tháng này!"}
	ReportRevenueNotFound = ErrorCode{Code: 404, Message: "Không tìm thấy báo cáo doanh thu!"}

	// Report Room Booking
	ReportRoomBookingExists   = ErrorCode{Code: 409, Message: "Báo cáo đã tồn tại trong tháng này!"}
	ReportRoomBookingNotFound = ErrorCode{Code: 404, Message: "Không tìm thấy báo cáo loại phòng!"}
)
