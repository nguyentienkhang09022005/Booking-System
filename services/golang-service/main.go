package main

import (
	"fmt"
	"github.com/gin-gonic/gin"
	"os"
	"payment-service/Configs"
	"payment-service/Routes"
	_ "payment-service/docs"
)

// @title Golang-Service API
// @version 1.0
// @host localhost:8080
// @BasePath /
func main() {
	// Kết nối database
	db := Configs.ConnectToDB()

	// Kết nối ZaloPay
	err := Configs.ZaloPayConfig()
	if err != nil {
		return
	}
	// Chạy Gin Framework
	r := gin.Default()

	// Thiết lập router
	Routes.SwaggerRouter(r)
	Routes.PaymentRouter(r, db)
	Routes.ReportRevenueRouter(r, db)

	// Chạy server
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}
	addr := fmt.Sprintf(":%s", port)
	r.Run(addr)
}
