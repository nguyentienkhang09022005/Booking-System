package Controllers

import (
	"github.com/gin-gonic/gin"
	"net/http"
	"payment-service/Internal/Dtos/Request"
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
	paymentResponse, err := r._reportRevenueService.CreateReportRevenue(reportRevenueRequest)
	if err != nil {
		Helper.Error(c, http.StatusInternalServerError, err.Error())
		return
	}
	Helper.Created(c, "Tạo báo cáo thu chi thành công!", paymentResponse)
}
