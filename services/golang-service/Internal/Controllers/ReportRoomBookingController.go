package Controllers

import (
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"net/http"
	"payment-service/Internal/Errors"
	"payment-service/Internal/Helper"
	"payment-service/Internal/Services"
	"strconv"
)

type reportRoomBookingController struct {
	_reportRoomBookingService Services.IReportRoomBookingService
}

func ReportRoomBookingController(reportRoomBookingService Services.IReportRoomBookingService) *reportRoomBookingController {
	return &reportRoomBookingController{_reportRoomBookingService: reportRoomBookingService}
}

// GetInfReportRoomBookingByMonth Lấy thông tin báo cáo loại phòng theo tháng
// @Tags Report Type Room Booking
// @Accept json
// @Produce json
// @Param idHotel query string true "ID Hotel" format:uuid
// @Param month query int true "Tháng"
// @Param year query int true "Năm"
// @Router /report-type-room-booking/inf-report-type-room-booking-by-month [get]
func (r *reportRoomBookingController) GetInfReportRoomBookingByMonth(c *gin.Context) {
	idHotelStr := c.Query("idHotel")
	monthStr := c.Query("month")
	yearStr := c.Query("year")

	if idHotelStr == "" || monthStr == "" || yearStr == "" {
		Helper.Error(c, http.StatusBadRequest, "Vui lòng cung cấp đầy đủ idHotel, month và year!")
		return
	}

	idHotelUUID, err := uuid.Parse(idHotelStr)
	if err != nil {
		Helper.Error(c, http.StatusBadRequest, "ID Hotel không đúng định dạng UUID!")
		return
	}

	month, err := strconv.Atoi(monthStr)
	if err != nil {
		Helper.Error(c, http.StatusBadRequest, "Month phải là số nguyên!")
		return
	}

	year, err := strconv.Atoi(yearStr)
	if err != nil {
		Helper.Error(c, http.StatusBadRequest, "Year phải là số nguyên!")
		return
	}

	reportRoomBookingResponse, err := r._reportRoomBookingService.GetInfReportRoomBookingByMonth(idHotelUUID, month, year)
	if err != nil {
		if customErr, ok := err.(*Errors.ApiError); ok {
			Helper.Error(c, customErr.Status, customErr.Message)
			return
		}

		Helper.Error(c, http.StatusInternalServerError, err.Error())
		return
	}
	Helper.Success(c, "Lấy thông tin báo cáo loại phòng thành công!", reportRoomBookingResponse)
}

// DeleteReportRoomBooking xóa báo cáo loại phòng
// @Tags Report Type Room Booking
// @Accept json
// @Produce json
// @Param idReportRoomBooking path string true "ID báo cáo"
// @Router /report-type-room-booking/{idReportRoomBooking} [delete]
func (r *reportRoomBookingController) DeleteReportRoomBooking(c *gin.Context) {
	idReportRoomBookingStr := c.Param("idReportRoomBooking")

	idReportRoomBooking, err := uuid.Parse(idReportRoomBookingStr)
	if err != nil {
		Helper.Error(c, http.StatusBadRequest, "ID báo cáo không hợp lệ (không đúng định dạng UUID)!")
		return
	}

	err = r._reportRoomBookingService.DeleteReportRoomBooking(idReportRoomBooking)
	if err != nil {

		if customErr, ok := err.(*Errors.ApiError); ok {
			Helper.Error(c, customErr.Status, customErr.Message)
			return
		}

		Helper.Error(c, http.StatusInternalServerError, err.Error())
		return
	}
	Helper.Success(c, "Xóa báo cáo loại phòng thành công!", nil)
}
