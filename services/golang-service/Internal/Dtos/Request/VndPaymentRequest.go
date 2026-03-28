package Request

import "github.com/google/uuid"

type PrepareVndPaymentRequest struct {
	IDUser             uuid.UUID  `json:"id_user" binding:"required"`
	IDBooking          *uuid.UUID `json:"id_booking"`
	IDBookingTour      *uuid.UUID `json:"id_booking_tour"`
	IDBookingTransport *uuid.UUID `json:"id_booking_transport"`
	Amount             float64    `json:"amount" binding:"required"`
	PayerAddress       string     `json:"payer_address" binding:"required"`
}

type PaymentOrderData struct {
	OrderID   string `json:"order_id"`
	Payer     string `json:"payer"`
	Amount    uint64 `json:"amount,string"`
	BookingID string `json:"booking_id"`
	Nonce     uint64 `json:"nonce"`
	Deadline  int64  `json:"deadline"`
}

type PermitData struct {
	V        uint8  `json:"v"`
	R        string `json:"r"`
	S        string `json:"s"`
	Value    string `json:"value"`
	Deadline int64  `json:"deadline"`
}

type ExecuteVndPaymentRequest struct {
	IDPayment string           `json:"id_payment" binding:"required"`
	Order     PaymentOrderData `json:"order" binding:"required"`
	Signature string           `json:"signature" binding:"required"`
	Permit    *PermitData      `json:"permit,omitempty"`
}
