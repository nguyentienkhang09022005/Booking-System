package Routes

import (
	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
	"payment-service/Internal/Controllers"
	"payment-service/Internal/Repositories"
	"payment-service/Internal/Services"
)

func ReportRoomBookingRouter(router *gin.Engine, db *gorm.DB) {
	reportRoomBookingRepository := Repositories.ReportRoomBookingRepository(db)
	accommodationRepository := Repositories.AccommodationRepository(db)

	reportRoomBookingService := Services.ReportRoomBookingService(reportRoomBookingRepository, accommodationRepository)

	reportRoomBookingController := Controllers.ReportRoomBookingController(reportRoomBookingService)

	reportRoomBookingRoutes := router.Group("/report-type-room-booking")
	{
		reportRoomBookingRoutes.GET("/inf-report-type-room-booking-by-month", reportRoomBookingController.GetInfReportRoomBookingByMonth)
		reportRoomBookingRoutes.DELETE("/:idReportRoomBooking", reportRoomBookingController.DeleteReportRoomBooking)
	}
}
