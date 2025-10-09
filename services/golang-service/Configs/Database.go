package Configs

import (
	"fmt"
	"github.com/joho/godotenv"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"log"
	"os"
)

func ConnectToDB() *gorm.DB {

	err := godotenv.Load(".env")
	if err != nil {
		log.Fatal("Load env thất bại!")
	}

	host := os.Getenv("DB_HOST")
	port := os.Getenv("DB_PORT")
	user := os.Getenv("DB_USER")
	password := os.Getenv("DB_PASSWORD")
	dbName := os.Getenv("DB_NAME")
	sslmode := os.Getenv("DB_SSLMODE")
	channelBinding := os.Getenv("DB_CHANNELBINDING")

	// Tạo chuỗi kết nối
	connectString := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=%s channelBinding=%s",
		host, port, user, password, dbName, sslmode, channelBinding)

	db, err := gorm.Open(postgres.Open(connectString), &gorm.Config{})
	if err != nil {
		log.Fatal("Kết nối database thất bại!", err)
	}
	fmt.Printf("Kết nối database thành công!")
	return db
}
