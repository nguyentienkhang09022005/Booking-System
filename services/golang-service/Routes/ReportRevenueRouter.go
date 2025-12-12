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

	reportRevenueService := Services.ReportRevenueService(reportRevenueRepository, hotelRepository)

	reportRevenueController := Controllers.ReportRevenueController(reportRevenueService)

	reportRevenueRoutes := router.Group("/report-revenues")
	{
		reportRevenueRoutes.GET("/inf-report-revenue-by-month", reportRevenueController.GetInfReportByMonth)
		reportRevenueRoutes.POST("/create", reportRevenueController.CreateReportRevenue)
		reportRevenueRoutes.DELETE("/:idReportRevenue", reportRevenueController.DeleteReportRevenue)
	}
}
