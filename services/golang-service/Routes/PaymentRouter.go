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

	reportRevenueRepository := Repositories.ReportRevenueRepository(db)
	reportRevenueDetailRepository := Repositories.ReportRevenueDetailRepository(db)

	hotelRepository := Repositories.HotelRepository(db)
	userRepository := Repositories.UserRepository(db)

	reportRevenueDetailService := Services.ReportRevenueDetailService(
		reportRevenueRepository,
		reportRevenueDetailRepository,
		hotelRepository,
		userRepository,
	)

	paymentService := Services.PaymentService(paymentRepository, reportRevenueDetailService)

	paymentController := Controllers.PaymentController(paymentService)

	paymentRoutes := router.Group("/api/v1/payments")
	{
		paymentRoutes.POST("/", paymentController.CreatePayment)
		paymentRoutes.POST("/callback", paymentController.HandleCallBack)
	}
}
