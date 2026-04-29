package main

import (
	"fmt"
	"github.com/gin-gonic/gin"
	"log"
	"os"
	"payment-service/Configs"
	"payment-service/Internal/Repositories"
	"payment-service/Internal/Services"
	"payment-service/Routes"
	_ "payment-service/docs"
)

func main() {
	db := Configs.ConnectToDB()

	if err := Configs.ZaloPayConfig(); err != nil {
		log.Printf("Warning: ZaloPay config failed (%v) — ZaloPay payment unavailable", err)
	}

	if err := Configs.BlockchainConfig(); err != nil {
		log.Printf("Warning: Blockchain config failed (%v) — VND Stablecoin payment unavailable", err)
	}

	reportRevenueRepository := Repositories.ReportRevenueRepository(db)
	reportRevenueDetailRepository := Repositories.ReportRevenueDetailRepository(db)
	accommodationRepository := Repositories.AccommodationRepository(db)
	reportRevenueDetailService := Services.ReportRevenueDetailService(reportRevenueRepository, reportRevenueDetailRepository, accommodationRepository)

	_ = Services.PaymentService(
		Repositories.PaymentRepository(db),
		reportRevenueDetailService,
	)

	r := gin.Default()

	Routes.SwaggerRouter(r)
	Routes.PaymentRouter(r, db)
	Routes.ReportRevenueRouter(r, db)
	Routes.ReportRevenueDetailRouter(r, db)
	Routes.ReportRoomBookingRouter(r, db)

	port := os.Getenv("PORT")
	if port == "" {
		port = "8081"
	}
	addr := fmt.Sprintf(":%s", port)
	r.Run(addr)
}
