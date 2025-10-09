package Controllers

import (
	"github.com/gin-gonic/gin"
	"net/http"
	"payment-service/Internal/Dtos/Request"
	"payment-service/Internal/Services"
)

type PaymentController struct {
	_paymentService Services.PaymentService
}

func NewPaymentController(paymentService Services.PaymentService) *PaymentController {
	return &PaymentController{_paymentService: paymentService}
}

// Controller tạo thanh toán
func (pc *PaymentController) CreatePayment(c *gin.Context) {
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
