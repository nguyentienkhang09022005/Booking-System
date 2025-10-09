package main

import (
	"github.com/gin-gonic/gin"
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
	r.Run(":8080")
}
