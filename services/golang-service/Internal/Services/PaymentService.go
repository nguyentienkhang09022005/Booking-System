package Services

import (
	"bytes"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/json"
	"fmt"
	"github.com/google/uuid"
	"io/ioutil"
	"net/http"
	"os"
	"payment-service/Configs"
	"payment-service/Constants"
	"payment-service/Internal/Dtos/Request"
	"payment-service/Internal/Dtos/Response"
	"payment-service/Internal/Models"
	"payment-service/Internal/Repositories"
	"time"
)

type IPaymentService interface {
	CreatePayment(paymentRequest Request.PaymentRequest) (*Response.PaymentResponse, error)
	ResolveCallBackZaloPay(zaloPayCallBackRequest Request.ZaloPayCallBackRequest) error
}

type paymentService struct {
	_paymentRepository          Repositories.IPaymentRepository
	_reportRevenueDetailService IReportRevenueDetailService
}

func PaymentService(paymentRepository Repositories.IPaymentRepository,
	reportRevenueDetailService IReportRevenueDetailService) IPaymentService {
	return &paymentService{
		_paymentRepository:          paymentRepository,
		_reportRevenueDetailService: reportRevenueDetailService,
	}
}

type ZaloPayCallBackData struct {
	AppID      int    `json:"app_id"`
	AppTransID string `json:"app_trans_id"`
	ZpTransID  int64  `json:"zp_trans_id"`
	AppTime    int64  `json:"app_time"`
	AppUser    string `json:"app_user"`
	Amount     int64  `json:"amount"`
}

// Hàm tạo thanh toán
func (p *paymentService) CreatePayment(paymentRequest Request.PaymentRequest) (*Response.PaymentResponse, error) {
	loc, _ := time.LoadLocation("Asia/Ho_Chi_Minh")
	prefix := time.Now().In(loc).Format("060102")
	randSuffix := time.Now().UnixNano() % 1000000
	IdAppTrans := fmt.Sprintf("%s_%06d", prefix, randSuffix)

	newPayment := &Models.Payment{
		IDPayment:          uuid.New(),
		IDUser:             paymentRequest.IDUser,
		IDBooking:          paymentRequest.IDBooking,
		IDBookingTour:      paymentRequest.IDBookingTour,
		IDBookingTransport: paymentRequest.IDBookingTransport,
		Amount:             paymentRequest.Amount,
		Method:             "ZaloPay",
		Status:             Constants.StatusPending,
		IDAppTrans:         IdAppTrans,
	}
	createdPayment, err := p._paymentRepository.Create(newPayment)
	if err != nil {
		return nil, fmt.Errorf("Không thể tạo thanh toán!: %w", err)
	}

	appTime := time.Now().UnixNano() / int64(time.Millisecond)
	embedData, _ := json.Marshal(map[string]string{
		"redirecturl": "http://localhost:3000/booking",
	})
	items, _ := json.Marshal([]map[string]interface{}{})

	appUser := paymentRequest.IDUser.String()

	dataToSign := fmt.Sprintf("%d|%s|%s|%d|%d|%s|%s",
		Configs.ZaloPayAppID,
		IdAppTrans,
		appUser,
		int64(paymentRequest.Amount),
		appTime,
		string(embedData),
		string(items),
	)

	mac := computeHmacSha256(dataToSign, Configs.ZaloPayKey1)

	// Xử lý description cho ZaloPay
	var orderDescription string
	if paymentRequest.IDBooking != nil {
		orderDescription = fmt.Sprintf("Thanh Toán Đơn Đặt Phòng #%s", paymentRequest.IDBooking.String())
	} else if paymentRequest.IDBookingTour != nil {
		orderDescription = fmt.Sprintf("Thanh Toán Đơn Đặt Tour #%s", paymentRequest.IDBookingTour.String())
	} else if paymentRequest.IDBookingTransport != nil {
		orderDescription = fmt.Sprintf("Thanh Toán Đơn Đặt Vé Di Chuyển #%s", paymentRequest.IDBookingTransport.String())
	} else {
		orderDescription = "Thanh Toán Dịch Vụ"
	}

	orderData := map[string]interface{}{
		"app_id":       Configs.ZaloPayAppID,
		"app_user":     appUser,
		"app_trans_id": IdAppTrans,
		"app_time":     appTime,
		"amount":       int64(paymentRequest.Amount),
		"item":         string(items),
		"description":  orderDescription,
		"embed_data":   string(embedData),
		"mac":          mac,
		"bank_code":    "zalopayapp",
		"callback_url": Configs.ZaloPayCallbackURL,
	}

	zaloPayResponse, err := sendCreateOrderRequest(orderData)
	if err != nil {
		p._paymentRepository.UpdateStatus(createdPayment.IDPayment, "", Constants.StatusFailed)
		return nil, err
	}

	if zaloPayResponse["return_code"] != float64(1) {
		p._paymentRepository.UpdateStatus(createdPayment.IDPayment, "", Constants.StatusFailed)
		return nil, fmt.Errorf("Thông báo ZaloPay: Tạo đặt hàng thất bại!: %s", zaloPayResponse["return_message"])
	}

	orderURL := zaloPayResponse["order_url"].(string)
	response := &Response.PaymentResponse{
		IDPayment: createdPayment.IDPayment,
		Amount:    createdPayment.Amount,
		Status:    createdPayment.Status,
		Method:    createdPayment.Method,
		OrderUrl:  orderURL,
		CreatedAt: createdPayment.CreateAt,
	}
	return response, nil
}

func (p *paymentService) ResolveCallBackZaloPay(zaloPayCallBackRequest Request.ZaloPayCallBackRequest) error {
	mac := computeHmacSha256(zaloPayCallBackRequest.Data, Configs.ZaloPayKey2)
	if mac != zaloPayCallBackRequest.Mac {
		return fmt.Errorf("Chữ ký không hợp lệ!")
	}

	var zaloPayCallBackData ZaloPayCallBackData
	err := json.Unmarshal([]byte(zaloPayCallBackRequest.Data), &zaloPayCallBackData)
	if err != nil {
		return fmt.Errorf("Không thể parse dữ liệu callback: %w", err)
	}

	payment, err := p._paymentRepository.FindByAppTransID(zaloPayCallBackData.AppTransID)
	if err != nil {
		return fmt.Errorf("Không tìm thấy giao dịch!")
	}

	if payment.Status == Constants.StatusSuccess {
		return nil
	}

	zpTransIdString := fmt.Sprintf("%d", zaloPayCallBackData.ZpTransID)
	err = p._paymentRepository.UpdateStatus(payment.IDPayment, zpTransIdString, Constants.StatusSuccess)
	if err != nil {
		return fmt.Errorf("Cập nhật trạng thái giao dịch thất bại!: %w", err)
	}

	now := time.Now()

	// Ghi nhận doanh thu cho Đặt phòng
	if payment.IDBooking != nil && payment.Booking != nil && payment.Booking.IDAccommodation != uuid.Nil {
		revenueEvent := Request.RevenueCreatedEvent{
			IDBooking:       payment.IDBooking,
			IDExpense:       nil,
			IDAccommodation: payment.Booking.IDAccommodation,
			Amount:          payment.Amount,
			Month:           int(now.Month()),
			Year:            now.Year(),
		}

		_, err = p._reportRevenueDetailService.CreateReportRevenueDetail(revenueEvent)
		if err != nil {
			return fmt.Errorf("Lỗi tạo báo cáo doanh thu phòng: %w", err)
		}
	}

	if payment.IDUser != uuid.Nil && payment.Amount > 0 {
		go p.triggerAddLoyaltyPoints(payment)
	}

	fmt.Println("Đã cập nhật thanh toán và tạo báo cáo thành công.")
	return nil
}

func (p *paymentService) triggerAddLoyaltyPoints(payment *Models.Payment) {
	javaServiceBaseURL := os.Getenv("JAVA_SERVICE")
	if javaServiceBaseURL == "" {
		javaServiceBaseURL = "http://localhost:8080"
	}

	javaServiceURL := fmt.Sprintf("%s/loyalty/add-points", javaServiceBaseURL)

	reqBody := map[string]interface{}{
		"idUser":             payment.IDUser,
		"amount":             payment.Amount,
		"idBooking":          payment.IDBooking,
		"idBookingTour":      payment.IDBookingTour,
		"idBookingTransport": payment.IDBookingTransport,
	}

	jsonBody, err := json.Marshal(reqBody)
	if err != nil {
		fmt.Printf("Lỗi parse JSON: %v\n", err)
		return
	}

	resp, err := http.Post(javaServiceURL, "application/json", bytes.NewBuffer(jsonBody))
	if err != nil {
		fmt.Printf("Lỗi khi gọi Java Service để cộng điểm: %v\n", err)
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK && resp.StatusCode != http.StatusCreated {
		fmt.Printf("Java Service phản hồi lỗi khi cộng điểm, Status Code: %d\n", resp.StatusCode)
	} else {
		fmt.Printf("Đã kích hoạt cộng điểm Loyalty thành công cho User: %s\n", payment.IDUser.String())
	}
}

// Hàm tạo chữ ký với Key 1
func computeHmacSha256(data string, key string) string {
	h := hmac.New(sha256.New, []byte(key))
	h.Write([]byte(data))
	return fmt.Sprintf("%x", h.Sum(nil))
}

// Hàm gửi yêu cầu thanh toán đến ZaloPay
func sendCreateOrderRequest(orderData map[string]interface{}) (map[string]interface{}, error) {
	requestBody, _ := json.Marshal(orderData)
	resp, err := http.Post(Configs.ZaloPayCreateOrderEndpoint, "application/json", bytes.NewBuffer(requestBody))
	if err != nil {
		return nil, fmt.Errorf("Gửi request đến ZaloPay thất bại!: %w", err)
	}
	defer resp.Body.Close()

	body, _ := ioutil.ReadAll(resp.Body)
	var result map[string]interface{}
	json.Unmarshal(body, &result)

	return result, nil
}
