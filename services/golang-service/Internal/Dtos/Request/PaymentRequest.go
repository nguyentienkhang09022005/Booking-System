package Request

import "github.com/google/uuid"

type PaymentRequest struct {
	IDUser    uuid.UUID `json:"id_user" binding:"required"`
	IDBooking uuid.UUID `json:"id_booking" binding:"required"`
	Amount    float64   `json:"amount" binding:"required"`
}
