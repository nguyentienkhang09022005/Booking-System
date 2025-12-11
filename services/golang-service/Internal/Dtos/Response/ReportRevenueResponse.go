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
	CreateAt        time.Time                     `json:"createAt"`
	Details         []ReportRevenueDetailResponse `json:"reportDetails,omitempty"`
}
