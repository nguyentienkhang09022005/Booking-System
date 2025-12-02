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
	reportRevenueService := Services.ReportRevenueService(reportRevenueRepository)
	reportRevenueController := Controllers.ReportRevenueController(reportRevenueService)

	reportRevenueRoutes := router.Group("/api/report-revenues")
	{
		reportRevenueRoutes.POST("/create", reportRevenueController.CreateReportRevenue)
	}
}
