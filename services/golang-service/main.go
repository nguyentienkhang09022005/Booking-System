package main

import (
	"fmt"
	"github.com/gin-gonic/gin"
	"os"
	"payment-service/Configs"
	"payment-service/Routes"
)

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
	Routes.PaymentRouter(r, db)

	// Chạy server
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}
	addr := fmt.Sprintf(":%s", port)
	r.Run(addr)
}
