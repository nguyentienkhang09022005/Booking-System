package Response

import (
	"github.com/google/uuid"
	"time"
)

type ReportRevenueDetailResponse struct {
	IDRevenueDetail uuid.UUID `json:"idRevenueDetail"`
	TypeRevenue     string    `json:"typeRevenue"`
	Amount          float64   `json:"amount"`
	CreateAt        time.Time `json:"createAt"`
}
