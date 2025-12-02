package Routes

import (
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
	"payment-service/Internal/Controllers"
	"payment-service/Internal/Repositories"
	"payment-service/Internal/Services"
)

func PaymentRouter(router *gin.Engine, db *gorm.DB) {
	paymentRepository := Repositories.PaymentRepository(db)
	paymentService := Services.PaymentService(paymentRepository)
	paymentController := Controllers.PaymentController(paymentService)

	paymentRoutes := router.Group("/api/v1/payments")
	{
		paymentRoutes.POST("/", paymentController.CreatePayment)
		paymentRoutes.POST("/callback", paymentController.HandleCallBack)
	}
}
