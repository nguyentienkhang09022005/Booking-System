package main

import (
	"github.com/gin-gonic/gin"
	"payment-service/Configs"
	"payment-service/Internal/Models"
)

func main() {
	// Kết nối database
	Configs.ConnectToDB()

	// Auto Migrate các bảng
	Configs.HotelDB.AutoMigrate(
		&Models.Booking{},
		&Models.IncidentReport{},
		&Models.Payment{},
		&Models.ReportRevenue{},
		&Models.ReportRevenueDetail{},
		&Models.ReportRoomTypeBooking{},
		&Models.User{},
	)

	// Chạy Gin Framework
	r := gin.Default()

	// Chạy server
	r.Run(":8080")
}
