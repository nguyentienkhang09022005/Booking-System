package RabbitMQ

import (
	"encoding/json"
	"github.com/joho/godotenv"
	"github.com/rabbitmq/amqp091-go"
	"log"
	"os"
	"payment-service/Internal/Dtos/Request"
	"payment-service/Internal/Services"
)

func StartRevenueConsumer(reportRevenueDetailService Services.IReportRevenueDetailService) {
	_ = godotenv.Load(".env")
	url_rabbitMQ := os.Getenv("URL_RABBITMQ")
	queueName := os.Getenv("QUEUE_NAME")

	conn, err := amqp091.Dial(url_rabbitMQ)
	if err != nil {
		log.Fatalf("Không thể kết nối đến RabbitMQ: %v", err)
	}

	ch, err := conn.Channel()
	if err != nil {
		log.Fatalf("Không thể tạo channel: %v", err)
	}

	// FIX: Khai báo queue trước khi consume
	_, err = ch.QueueDeclare(
		queueName,
		true,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		log.Fatalf("Không thể khai báo queue: %v", err)
	}

	msgs, err := ch.Consume(
		queueName,
		"",
		true,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		log.Fatalf("Không thể bắt đầu tiêu thụ message: %v", err)
	}

	log.Println("Consumer đã khởi động, đang lắng nghe các sự kiện doanh thu!")

	go func() {
		for m := range msgs {
			log.Printf("Nhận message: %s\n", string(m.Body))

			var event Request.RevenueCreatedEvent
			if err := json.Unmarshal(m.Body, &event); err != nil {
				log.Printf("Lỗi giải mã JSON: %v\n", err)
				continue
			}

			_, err := reportRevenueDetailService.CreateReportRevenueDetail(event)
			if err != nil {
				log.Printf("Xử lý sự kiện doanh thu thất bại: %v\n", err)
				continue
			}

			log.Println("Xử lý sự kiện doanh thu thành công")
		}
	}()
}
