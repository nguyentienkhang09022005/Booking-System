package main

import (
	"fmt"
	"github.com/gin-gonic/gin"
	"log"
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
// @BasePath /
func main() {
	// Kết nối database
	db := Configs.ConnectToDB()

	// Kết nối ZaloPay
	err := Configs.ZaloPayConfig()
	if err != nil {
		return
	}

	// Kết nối Blockchain (Sepolia)
	if err := Configs.BlockchainConfig(); err != nil {
		log.Printf("Warning: Blockchain config failed (%v) — VND Stablecoin payment unavailable", err)
	}

	reportRevenueRepository := Repositories.ReportRevenueRepository(db)
	reportRevenueDetailRepository := Repositories.ReportRevenueDetailRepository(db)
	accommodationRepository := Repositories.AccommodationRepository(db)
	reportRevenueDetailService := Services.ReportRevenueDetailService(reportRevenueRepository, reportRevenueDetailRepository, accommodationRepository)

	// Khởi động RabbitMQ consumer
	go func() {
		log.Println("Starting Revenue Consumer...")
		RabbitMQ.StartRevenueConsumer(reportRevenueDetailService)
	}()

	// Chạy Gin Framework
	r := gin.Default()

	// Thiết lập router
	Routes.SwaggerRouter(r)
	Routes.PaymentRouter(r, db)
	Routes.ReportRevenueRouter(r, db)
	Routes.ReportRevenueDetailRouter(r, db)
	Routes.ReportRoomBookingRouter(r, db)

	// Chạy server
	port := os.Getenv("PORT")
	if port == "" {
		port = "8081"
	}
	addr := fmt.Sprintf(":%s", port)
	r.Run(addr)
}
