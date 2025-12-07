package Response

import (
	"github.com/google/uuid"
	"time"
)

type ReportRevenueResponse struct {
	IDReportRevenue uuid.UUID                     `json:"idReportRevenue"`
	Month           int                           `json:"month"`
	Year            int                           `json:"year"`
	TotalRevenue    float64                       `json:"totalRevenue"`
	TotalIncome     float64                       `json:"totalIncome"`
	TotalExpense    float64                       `json:"totalExpense"`
	Profit          float64                       `json:"profit"`
	CreateAt        time.Time                     `json:"createAt"`
	Details         []ReportRevenueDetailResponse `json:"reportDetails,omitempty"`
}
