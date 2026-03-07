package Request

import (
	"github.com/google/uuid"
)

type ReportRevenueRequest struct {
	IDAccommodation uuid.UUID `json:"idAccommodation"`
	Month           int       `json:"month"`
	Year            int       `json:"year"`
}
