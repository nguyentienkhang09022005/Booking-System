package Routes

import (
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
	"payment-service/Internal/Controllers"
	"payment-service/Internal/Repositories"
	"payment-service/Internal/Services"
)

func ReportRevenueRouter(router *gin.Engine, db *gorm.DB) {
	reportRevenueRepository := Repositories.ReportRevenueRepository(db)
	hotelRepository := Repositories.HotelRepository(db)
	userRepository := Repositories.UserRepository(db)

	reportRevenueService := Services.ReportRevenueService(reportRevenueRepository, hotelRepository, userRepository)
	reportRevenueController := Controllers.ReportRevenueController(reportRevenueService)

	reportRevenueRoutes := router.Group("/api/report-revenues")
	{
		reportRevenueRoutes.POST("/create", reportRevenueController.CreateReportRevenue)
		reportRevenueRoutes.DELETE("/:idReportRevenue", reportRevenueController.DeleteReportRevenue)
	}
}
