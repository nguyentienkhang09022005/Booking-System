package Request

import "github.com/google/uuid"

type RevenueUpdateEvent struct {
	IDExpense uuid.UUID  `json:"idExpense"`
	IDBooking *uuid.UUID `json:"idBooking"`
	Amount    float64    `json:"amount"`
}
