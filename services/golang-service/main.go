package main

import (
	"fmt"
	"github.com/gin-gonic/gin"
	"os"
	"payment-service/Configs"
	"payment-service/Internal/RabbitMQ"
	"payment-service/Internal/Repositories"
	"payment-service/Internal/Services"
	"payment-service/Routes"
	_ "payment-service/docs"
)

// @title Golang-Service API
// @version 1.0
// @host localhost:8081
// @BasePath /
func main() {
	// Kết nối database
	db := Configs.ConnectToDB()

	// Kết nối ZaloPay
	err := Configs.ZaloPayConfig()
	if err != nil {
		return
	}

	reportRevenueRepository := Repositories.ReportRevenueRepository(db)
	reportRevenueDetailRepository := Repositories.ReportRevenueDetailRepository(db)
	userRepository := Repositories.UserRepository(db)
	hotelRepository := Repositories.HotelRepository(db)
	reportRevenueDetailService := Services.ReportRevenueDetailService(reportRevenueRepository, reportRevenueDetailRepository, hotelRepository, userRepository)

	// Khởi động RabbitMQ consumer
	go RabbitMQ.StartRevenueConsumer(reportRevenueDetailService)

	// Chạy Gin Framework
	r := gin.Default()

	// Thiết lập router
	Routes.SwaggerRouter(r)
	Routes.PaymentRouter(r, db)
	Routes.ReportRevenueRouter(r, db)
	Routes.ReportRevenueDetailRouter(r, db)

	// Chạy server
	port := os.Getenv("PORT")
	if port == "" {
		port = "8081"
	}
	addr := fmt.Sprintf(":%s", port)
	r.Run(addr)
}
