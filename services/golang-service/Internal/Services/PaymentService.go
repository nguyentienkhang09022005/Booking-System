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
	"payment-service/Configs"
	"payment-service/Internal/Dtos/Request"
	"payment-service/Internal/Dtos/Response"
	"payment-service/Internal/Models"
	"payment-service/Internal/Repositories"
	"time"
)

type PaymentService interface {
	CreatePayment(paymentRequest Request.PaymentRequest) (*Response.PaymentResponse, error)
	ResolveCallBackZaloPay(zaloPayCallBackRequest Request.ZaloPayCallBackRequest) error
}

type paymentService struct {
	_paymentRepository Repositories.PaymentRepository
}

func NewPaymentService(paymentRepository Repositories.PaymentRepository) PaymentService {
	return &paymentService{_paymentRepository: paymentRepository}
}

type ZaloPayCallBackData struct {
	AppID      int    `json:"app_id"`
	AppTransID string `json:"app_trans_id"`
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
		IDPayment:  uuid.New(),
		IDUser:     paymentRequest.IDUser,
		IDBooking:  paymentRequest.IDBooking,
		Amount:     paymentRequest.Amount,
		Method:     "ZaloPay",
		Status:     "Pending",
		IDAppTrans: IdAppTrans,
	}
	createdPayment, err := p._paymentRepository.Create(newPayment)
	if err != nil {
		return nil, fmt.Errorf("Không thể tạo thanh toán!: %w", err)
	}

	// Chuẩn bị và gọi ZaloPay API
	appTime := time.Now().UnixNano() / int64(time.Millisecond)
	embedData, _ := json.Marshal(map[string]string{})
	items, _ := json.Marshal([]map[string]interface{}{})

	appUser := paymentRequest.IDUser.String()

	// Dữ liệu tạo chữ ký
	dataToSign := fmt.Sprintf("%d|%s|%s|%d|%d|%s|%s",
		Configs.ZaloPayAppID,
		IdAppTrans,
		appUser,
		int64(paymentRequest.Amount),
		appTime,
		string(embedData),
		string(items),
	)

	// Tạo chữ ký với Key 1
	mac := computeHmacSha256(dataToSign, Configs.ZaloPayKey1)

	orderData := map[string]interface{}{
		"app_id":       Configs.ZaloPayAppID,
		"app_user":     paymentRequest.IDUser.String(),
		"app_trans_id": IdAppTrans,
		"app_time":     appTime,
		"amount":       int64(paymentRequest.Amount),
		"item":         string(items),
		"description":  fmt.Sprintf("Thanh Toán Đơn Đặt Phòng #%s", paymentRequest.IDBooking),
		"embed_data":   string(embedData),
		"mac":          mac,
		"bank_code":    "zalopayapp",
		"callback_url": Configs.ZaloPayCallbackURL,
	}

	// Gửi request
	zaloPayResponse, err := sendCreateOrderRequest(orderData)
	if err != nil {
		// Cập nhật status thành "failed" nếu gọi ZaloPay lỗi
		p._paymentRepository.UpdateStatus(createdPayment.IDPayment, "Failed")
		return nil, err
	}

	if zaloPayResponse["return_code"] != float64(1) {
		p._paymentRepository.UpdateStatus(createdPayment.IDPayment, "Failed")
		return nil, fmt.Errorf("Thông báo ZaloPay: Tạo đặt hàng thất bại!: %s", zaloPayResponse["return_message"])
	}

	// Tạo response trả về cho client
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
	// Tạo chữ ký với Key 2
	mac := computeHmacSha256(zaloPayCallBackRequest.Data, Configs.ZaloPayKey2)
	if mac != zaloPayCallBackRequest.Mac {
		return fmt.Errorf("Chữ ký không hợp lệ!")
	}

	// Parse dữ liệu
	var zaloPayCallBackData ZaloPayCallBackData
	err := json.Unmarshal([]byte(zaloPayCallBackRequest.Data), &zaloPayCallBackData)
	if err != nil {
		return fmt.Errorf("Không thể parse dữ liệu callback: %w", err)
	}

	// Tìm giao dịch trong db
	payment, err := p._paymentRepository.FindByAppTransID(zaloPayCallBackData.AppTransID)
	if err != nil {
		return fmt.Errorf("Không tìm thấy giao dịch!")
	}

	if payment.Status == "Success" {
		return nil
	}

	// Cập nhật trạng thái thành công
	err = p._paymentRepository.UpdateStatus(payment.IDPayment, "Success")
	if err != nil {
		return fmt.Errorf("Cập nhật trạng thái giao dịch thất bại!: %w", err)
	}
	return nil
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
