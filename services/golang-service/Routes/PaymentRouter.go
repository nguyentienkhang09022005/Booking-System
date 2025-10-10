package Routes

import (
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
	"payment-service/Internal/Controllers"
	"payment-service/Internal/Repositories"
	"payment-service/Internal/Services"
)

func PaymentRouter(router *gin.Engine, db *gorm.DB) {
	paymentRepository := Repositories.NewPaymentRepository(db)
	paymentService := Services.NewPaymentService(paymentRepository)
	paymentController := Controllers.NewPaymentController(paymentService)

	paymentRoutes := router.Group("/api/payments")
	{
		paymentRoutes.POST("", paymentController.CreatePayment)
		paymentRoutes.POST("/callback", paymentController.CreatePayment)
	}
}
