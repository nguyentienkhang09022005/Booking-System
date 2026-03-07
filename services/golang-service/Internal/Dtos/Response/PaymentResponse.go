package Response

import (
	"github.com/google/uuid"
	"time"
)

type PaymentResponse struct {
	IDPayment uuid.UUID `json:"id_payment"`
	Amount    float64   `json:"amount"`
	Method    string    `json:"method"`
	Status    string    `json:"status"`
	Currency  string    `json:"currency"`
	OrderUrl  string    `json:"order_url,omitempty"`
	CreatedAt time.Time `json:"created_at"`
}
