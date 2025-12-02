package Controllers

import (
	"github.com/gin-gonic/gin"
	"net/http"
	"payment-service/Internal/Dtos/Request"
	"payment-service/Internal/Services"
)

type paymentController struct {
	_paymentService Services.IPaymentService
}

func PaymentController(paymentService Services.IPaymentService) *paymentController {
	return &paymentController{_paymentService: paymentService}
}

// CreatePayment
// @Tags Payment
// @Accept json
// @Produce json
// @Param request body Request.PaymentRequest true "Thông tin thanh toán"
// @Router /api/v1/payments [post]
func (pc *paymentController) CreatePayment(c *gin.Context) {
	var paymentRequest Request.PaymentRequest
	if err := c.ShouldBindJSON(&paymentRequest); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	paymentResponse, err := pc._paymentService.CreatePayment(paymentRequest)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusCreated, paymentResponse)
}

// HandleCallBack
// @Tags Payment
// @Accept json
// @Produce json
// @Param request body Request.ZaloPayCallBackRequest true "Dữ liệu callback"
// @Router /api/v1/payments/callback [post]
func (pc *paymentController) HandleCallBack(c *gin.Context) {
	var zaloPayCallBackRequest Request.ZaloPayCallBackRequest
	if err := c.ShouldBind(&zaloPayCallBackRequest); err != nil {
		c.JSON(http.StatusOK, gin.H{"return_code": 0, "return_message": "Dữ liệu không hợp lệ!"})
		return
	}

	err := pc._paymentService.ResolveCallBackZaloPay(zaloPayCallBackRequest)
	if err != nil {

		c.JSON(http.StatusOK, gin.H{"return_code": 0, "return_message": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"return_code": 1, "return_message": "Thành công"})
}
