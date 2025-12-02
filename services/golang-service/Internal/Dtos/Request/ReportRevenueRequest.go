package Request

import (
	"github.com/google/uuid"
)

type ReportRevenueRequest struct {
	IDHotel uuid.UUID `gorm:"type:uuid" json:"idHotel"`
	IDUser  uuid.UUID `gorm:"type:uuid" json:"idUser"`
	Month   int       `json:"month"`
	Year    int       `json:"year"`
}
