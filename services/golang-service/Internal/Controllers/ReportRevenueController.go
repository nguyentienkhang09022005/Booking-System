package Controllers

import (
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"net/http"
	"payment-service/Internal/Dtos/Request"
	"payment-service/Internal/Errors"
	"payment-service/Internal/Helper"
	"payment-service/Internal/Services"
)

type reportRevenueController struct {
	_reportRevenueService Services.IReportRevenueService
}

func ReportRevenueController(reportRevenueService Services.IReportRevenueService) *reportRevenueController {
	return &reportRevenueController{_reportRevenueService: reportRevenueService}
}

// CreateReportRevenue tạo báo cáo thu chi mới
// @Tags Report Revenue
// @Accept json
// @Produce json
// @Param request body Request.ReportRevenueRequest true "Thông tin báo cáo"
// @Router /api/report-revenues/create [post]
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
// @Param idReportRevenue path string true "ID báo cáo (UUID)"
// @Router /api/report-revenues/{idReportRevenue} [delete]
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
