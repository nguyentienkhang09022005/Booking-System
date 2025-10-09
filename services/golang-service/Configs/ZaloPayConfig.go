package Configs

import (
	"fmt"
	"github.com/joho/godotenv"
	"log"
	"os"
	"strconv"
)

var (
	ZaloPayAppID               int64
	ZaloPayKey1                string
	ZaloPayKey2                string
	ZaloPayCreateOrderEndpoint string
	ZaloPayCallbackURL         string
)

func ZaloPayConfig() error {
	if err := godotenv.Load(".env"); err != nil {
		log.Println("Lỗi: không tìm thấy file .env!")
	}

	// Đọc AppID
	var err error
	appIDStr := os.Getenv("ZALOPAY_APP_ID")

	ZaloPayAppID, err = strconv.ParseInt(appIDStr, 10, 64)
	if err != nil {
		log.Fatalf("Error parsing ZALOPAY_APP_ID: %v", err)
	}

	// Đọc các key và endpoint
	ZaloPayKey1 = os.Getenv("ZALOPAY_KEY1")
	ZaloPayKey2 = os.Getenv("ZALOPAY_KEY2")
	ZaloPayCreateOrderEndpoint = os.Getenv("ZALOPAY_CREATE_ORDER_ENDPOINT")

	// Tạo CallbackURL từ domain của app
	appDomain := os.Getenv("APP_DOMAIN")
	ZaloPayCallbackURL = fmt.Sprintf("%s/api/v1/payments/callback", appDomain)

	if ZaloPayKey1 == "" || ZaloPayKey2 == "" {
		return fmt.Errorf("Lỗi: Key 1, Key 2 phải được cài đặt!")
	}

	return nil
}
