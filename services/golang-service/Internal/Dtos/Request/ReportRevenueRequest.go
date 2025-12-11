package Request

import (
	"github.com/google/uuid"
)

type ReportRevenueRequest struct {
	IDHotel uuid.UUID `json:"idHotel"`
	Month   int       `json:"month"`
	Year    int       `json:"year"`
}
