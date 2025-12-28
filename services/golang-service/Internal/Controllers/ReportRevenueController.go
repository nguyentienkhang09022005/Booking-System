package Controllers

import (
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"net/http"
	"payment-service/Internal/Dtos/Request"
	"payment-service/Internal/Errors"
	"payment-service/Internal/Helper"
	"payment-service/Internal/Services"
	"strconv"
)

type reportRevenueController struct {
	_reportRevenueService Services.IReportRevenueService
}

func ReportRevenueController(reportRevenueService Services.IReportRevenueService) *reportRevenueController {
	return &reportRevenueController{_reportRevenueService: reportRevenueService}
}

// GetInfReportByMonth Lấy thông báo cáo doanh thu theo tháng
// @Tags Report Revenue
// @Accept json
// @Produce json
// @Param idHotel query string true "ID Hotel" format:uuid
// @Param month query int true "Tháng"
// @Param year query int true "Năm"
// @Router /report-revenues/inf-report-revenue-by-month [get]
func (r *reportRevenueController) GetInfReportByMonth(c *gin.Context) {
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

	reportRevenueResponse, err := r._reportRevenueService.GetInfReportByMonth(idHotelUUID, month, year)
	if err != nil {
		if customErr, ok := err.(*Errors.ApiError); ok {
			Helper.Error(c, customErr.Status, customErr.Message)
			return
		}

		Helper.Error(c, http.StatusInternalServerError, err.Error())
		return
	}
	Helper.Success(c, "Lấy thông tin báo cáo doanh thu thành công!", reportRevenueResponse)
}

// CreateReportRevenue tạo báo cáo doanh thu mới
// @Tags Report Revenue
// @Accept json
// @Produce json
// @Param request body Request.ReportRevenueRequest true "Thông tin báo cáo"
// @Router /report-revenues/create [post]
func (r *reportRevenueController) CreateReportRevenue(c *gin.Context) {
	var reportRevenueRequest Request.ReportRevenueRequest
	if err := c.ShouldBindJSON(&reportRevenueRequest); err != nil {
		Helper.Error(c, http.StatusBadRequest, "Dữ liệu gửi lên không hợp lệ: "+err.Error())
		return
	}
	reportRevenueResponse, err := r._reportRevenueService.CreateReportRevenue(reportRevenueRequest)
	if err != nil {
		if customErr, ok := err.(*Errors.ApiError); ok {
			Helper.Error(c, customErr.Status, customErr.Message)
			return
		}

		Helper.Error(c, http.StatusInternalServerError, err.Error())
		return
	}
	Helper.Created(c, "Tạo báo cáo doanh thu thành công!", reportRevenueResponse)
}

// DeleteReportRevenue xóa báo cáo doanh thu
// @Tags Report Revenue
// @Accept json
// @Produce json
// @Param idReportRevenue path string true "ID báo cáo"
// @Router /report-revenues/{idReportRevenue} [delete]
func (r *reportRevenueController) DeleteReportRevenue(c *gin.Context) {
	idReportRevenueStr := c.Param("idReportRevenue")

	idReportRevenue, err := uuid.Parse(idReportRevenueStr)
	if err != nil {
		Helper.Error(c, http.StatusBadRequest, "ID báo cáo không hợp lệ (không đúng định dạng UUID)!")
		return
	}

	err = r._reportRevenueService.DeleteReportRevenue(idReportRevenue)
	if err != nil {

		if customErr, ok := err.(*Errors.ApiError); ok {
			Helper.Error(c, customErr.Status, customErr.Message)
			return
		}

		Helper.Error(c, http.StatusInternalServerError, err.Error())
		return
	}
	Helper.Success(c, "Xóa báo cáo doanh thu thành công!", nil)
}

// GetYearlyRevenueStats Lấy thống kê tổng thu và chi theo từng tháng trong năm
// @Tags Report Revenue
// @Accept json
// @Produce json
// @Param idHotel query string true "ID Hotel" format:uuid
// @Param year query int true "Năm"
// @Router /report-revenues/yearly-stats [get]
func (r *reportRevenueController) GetYearlyRevenueStats(c *gin.Context) {
	idHotelStr := c.Query("idHotel")
	yearStr := c.Query("year")

	if idHotelStr == "" || yearStr == "" {
		Helper.Error(c, http.StatusBadRequest, "Vui lòng cung cấp đầy đủ idHotel và year!")
		return
	}

	idHotelUUID, err := uuid.Parse(idHotelStr)
	if err != nil {
		Helper.Error(c, http.StatusBadRequest, "ID Hotel không đúng định dạng UUID!")
		return
	}

	year, err := strconv.Atoi(yearStr)
	if err != nil {
		Helper.Error(c, http.StatusBadRequest, "Year phải là số nguyên!")
		return
	}

	stats, err := r._reportRevenueService.GetYearlyRevenueStats(idHotelUUID, year)
	if err != nil {
		if customErr, ok := err.(*Errors.ApiError); ok {
			Helper.Error(c, customErr.Status, customErr.Message)
			return
		}

		Helper.Error(c, http.StatusInternalServerError, err.Error())
		return
	}

	Helper.Success(c, "Lấy thống kê thu chi năm thành công!", stats)
}
