package Controllers

import (
	"github.com/gin-gonic/gin"
	"net/http"
	"payment-service/Internal/Dtos/Request"
	"payment-service/Internal/Errors"
	"payment-service/Internal/Helper"
	"payment-service/Internal/Services"
)

type reportRevenueDetailController struct {
	_reportRevenueDetailService Services.IReportRevenueDetailService
}

func ReportRevenueDetailController(reportRevenueDetailService Services.IReportRevenueDetailService) *reportRevenueDetailController {
	return &reportRevenueDetailController{_reportRevenueDetailService: reportRevenueDetailService}
}

// CreateReportRevenueDetail tạo chi tiết báo cáo doanh thu
// @Tags Report Revenue Detail
// @Accept json
// @Produce json
// @Param request body Request.RevenueCreatedEvent true "Thông tin chi tiết báo cáo"
// @Router /report-revenues-detail/create [post]
func (r *reportRevenueDetailController) CreateReportRevenueDetail(c *gin.Context) {
	var revenueCreatedEvent Request.RevenueCreatedEvent
	if err := c.ShouldBindJSON(&revenueCreatedEvent); err != nil {
		Helper.Error(c, http.StatusBadRequest, "Dữ liệu gửi lên không hợp lệ: "+err.Error())
		return
	}
	reportRevenueDetailResponse, err := r._reportRevenueDetailService.CreateReportRevenueDetail(revenueCreatedEvent)
	if err != nil {
		if customErr, ok := err.(*Errors.ApiError); ok {
			Helper.Error(c, customErr.Status, customErr.Message)
			return
		}

		Helper.Error(c, http.StatusInternalServerError, err.Error())
		return
	}
	Helper.Created(c, "Tạo chi tiết báo cáo doanh thu thành công!", reportRevenueDetailResponse)
}

// UpdateReportRevenueDetail cập nhật chi tiết báo cáo doanh thu
// @Tags Report Revenue Detail
// @Accept json
// @Produce json
// @Param request body Request.RevenueUpdateEvent true "Thông tin cập nhật"
// @Router /report-revenues-detail/update [put]
func (r *reportRevenueDetailController) UpdateReportRevenueDetail(c *gin.Context) {
	var revenueUpdateEvent Request.RevenueUpdateEvent
	if err := c.ShouldBindJSON(&revenueUpdateEvent); err != nil {
		Helper.Error(c, http.StatusBadRequest, "Dữ liệu gửi lên không hợp lệ: "+err.Error())
		return
	}
	err := r._reportRevenueDetailService.UpdateReportRevenueDetail(revenueUpdateEvent)
	if err != nil {
		if customErr, ok := err.(*Errors.ApiError); ok {
			Helper.Error(c, customErr.Status, customErr.Message)
			return
		}

		Helper.Error(c, http.StatusInternalServerError, err.Error())
		return
	}
	Helper.Success(c, "Cập nhật chi tiết báo cáo doanh thu thành công!", nil)
}
