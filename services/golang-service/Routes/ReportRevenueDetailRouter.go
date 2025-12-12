package Routes

import (
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
	"payment-service/Internal/Controllers"
	"payment-service/Internal/Repositories"
	"payment-service/Internal/Services"
)

func ReportRevenueDetailRouter(router *gin.Engine, db *gorm.DB) {
	reportRevenueRepository := Repositories.ReportRevenueRepository(db)
	reportRevenueDetailRepository := Repositories.ReportRevenueDetailRepository(db)
	hotelRepository := Repositories.HotelRepository(db)

	reportRevenueDetailService := Services.ReportRevenueDetailService(reportRevenueRepository, reportRevenueDetailRepository, hotelRepository)

	reportRevenueDetailController := Controllers.ReportRevenueDetailController(reportRevenueDetailService)

	reportRevenueRoutes := router.Group("/report-revenues-detail")
	{
		reportRevenueRoutes.POST("/create", reportRevenueDetailController.CreateReportRevenueDetail)
	}
}
