package Request

import "github.com/google/uuid"

type RevenueCreatedEvent struct {
	IDExpense *uuid.UUID `json:"idExpense"`
	IDBooking *uuid.UUID `json:"idBooking"`
	IDHotel   uuid.UUID  `json:"idHotel"`
	IDUser    uuid.UUID  `json:"idUser"`
	Amount    float64    `json:"amount"`
	Month     int        `json:"month"`
	Year      int        `json:"year"`
}
