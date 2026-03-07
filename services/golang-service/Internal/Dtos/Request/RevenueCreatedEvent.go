package Request

import "github.com/google/uuid"

type RevenueCreatedEvent struct {
	IDExpense       *uuid.UUID `json:"idExpense"`
	IDBooking       *uuid.UUID `json:"idBooking"`
	IDAccommodation uuid.UUID  `json:"idAccommodation"`
	Amount          float64    `json:"amount"`
	Month           int        `json:"month"`
	Year            int        `json:"year"`
}
