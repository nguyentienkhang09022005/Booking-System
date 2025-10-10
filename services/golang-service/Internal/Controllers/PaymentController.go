package Controllers

import (
	"github.com/gin-gonic/gin"
	"log"
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

func (pc *PaymentController) HandleCallBack(c *gin.Context) {
	var zaloPayCallBackRequest Request.ZaloPayCallBackRequest
	if err := c.ShouldBind(&zaloPayCallBackRequest); err != nil {
		log.Printf("[ZALO_CALLBACK_ERROR] Dữ liệu bind lỗi: %v", err)
		c.JSON(http.StatusOK, gin.H{"return_code": 0, "return_message": "Dữ liệu không hợp lệ!"})
		return
	}
	log.Printf("[ZALO_CALLBACK_INFO] Dữ liệu nhận được: Data=%s, Mac=%s", zaloPayCallBackRequest.Data, zaloPayCallBackRequest.Mac)

	err := pc._paymentService.ResolveCallBackZaloPay(zaloPayCallBackRequest)
	if err != nil {
		log.Printf("[ZALO_CALLBACK_ERROR] Xử lý callback thất bại: %v", err)

		c.JSON(http.StatusOK, gin.H{"return_code": 0, "return_message": err.Error()})
		return
	}
	log.Println("[ZALO_CALLBACK_SUCCESS] Xử lý callback thành công!")

	c.JSON(http.StatusOK, gin.H{"return_code": 1, "return_message": "Thành công"})
}
