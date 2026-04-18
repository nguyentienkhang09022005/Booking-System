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
	queueNameUpdate := os.Getenv("QUEUE_NAME_UPDATE")

	conn, err := amqp091.Dial(url_rabbitMQ)
	if err != nil {
		log.Printf("Không thể kết nối đến RabbitMQ: %v — Revenue consumer unavailable", err)
		return
	}
	defer conn.Close()

	ch, err := conn.Channel()
	if err != nil {
		log.Printf("Không thể tạo channel: %v — Revenue consumer unavailable", err)
		return
	}
	defer ch.Close()

	_, err = ch.QueueDeclare(
		queueName,
		true,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		log.Printf("Không thể khai báo queue create: %v — Revenue consumer unavailable", err)
		return
	}

	msgsCreate, err := ch.Consume(
		queueName,
		"",
		true,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		log.Printf("Không thể bắt đầu tiêu thụ queue create: %v — Revenue consumer unavailable", err)
		return
	}

	_, err = ch.QueueDeclare(
		queueNameUpdate,
		true,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		log.Printf("Không thể khai báo queue update: %v — Revenue consumer unavailable", err)
		return
	}

	msgsUpdate, err := ch.Consume(
		queueNameUpdate,
		"",
		true,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		log.Printf("Không thể bắt đầu tiêu thụ queue update: %v — Revenue consumer unavailable", err)
		return
	}

	log.Println("Consumer đã khởi động, đang lắng nghe cả sự kiện tạo và cập nhật doanh thu!")

	// Goroutine 1: Xử lý sự kiện create
	go func() {
		for m := range msgsCreate {
			log.Printf("[create] Nhận message: %s\n", string(m.Body))

			var event Request.RevenueCreatedEvent
			if err := json.Unmarshal(m.Body, &event); err != nil {
				log.Printf("[create] Lỗi giải mã JSON: %v\n", err)
				continue
			}

			_, err := reportRevenueDetailService.CreateReportRevenueDetail(event)
			if err != nil {
				log.Printf("[create] Xử lý thất bại: %v\n", err)
				continue
			}

			log.Println("[create] Xử lý thành công")
		}
	}()

	// Goroutine 2: Xử lý sự kiện update
	go func() {
		for m := range msgsUpdate {
			log.Printf("[update] Nhận message: %s\n", string(m.Body))

			var event Request.RevenueUpdateEvent
			if err := json.Unmarshal(m.Body, &event); err != nil {
				log.Printf("[update] Lỗi giải mã JSON: %v\n", err)
				continue
			}

			err := reportRevenueDetailService.UpdateReportRevenueDetail(event)
			if err != nil {
				log.Printf("[update] Xử lý thất bại: %v\n", err)
				continue
			}

			log.Println("[update] Xử lý thành công")
		}
	}()

	// Block this goroutine so defers don't fire while consumers are active.
	// The goroutine spawned in main.go will keep running until the process exits.
	select {}
}
