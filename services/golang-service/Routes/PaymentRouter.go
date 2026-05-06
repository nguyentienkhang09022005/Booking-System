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

	accommodationRepository := Repositories.AccommodationRepository(db)

	reportRevenueDetailService := Services.ReportRevenueDetailService(
		reportRevenueRepository,
		reportRevenueDetailRepository,
		accommodationRepository)

	paymentService := Services.PaymentService(paymentRepository, reportRevenueDetailService)
	blockchainService := Services.BlockchainService()

	paymentController := Controllers.PaymentController(paymentService)
	vndPaymentController := Controllers.VndPaymentController(blockchainService, paymentRepository, reportRevenueDetailService)

	// ZaloPay routes (existing)
	paymentRoutes := router.Group("/api/v1/payments")
	{
		paymentRoutes.POST("/", paymentController.CreatePayment)
		paymentRoutes.POST("/callback", paymentController.HandleCallBack)
	}

	// Web3 / VND Stablecoin routes — namespaced separately from /api/v1/payments
	// to avoid the deployed gateway routing /api/v1/** to the Java service.
	vndRoutes := router.Group("/web3/payment")
	{
		vndRoutes.POST("/prepare", vndPaymentController.PrepareVndPayment)
		vndRoutes.POST("/execute", vndPaymentController.ExecuteVndPayment)
		vndRoutes.GET("/nonce/:address", vndPaymentController.GetNonce)
	}
}
