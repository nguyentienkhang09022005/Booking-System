package Models

import (
	"time"

	"github.com/google/uuid"
)

type ReportRevenue struct {
	IDReportRevenue uuid.UUID `gorm:"type:uuid;default:uuid_generate_v4();primaryKey" json:"id_report_revenue"`
	Month           int       `json:"month"`
	Year            int       `json:"year"`
	TotalRevenue    float64   `gorm:"type:numeric(15,2)" json:"total_revenue"`
	CreateAt        time.Time `gorm:"type:timestamp;default:current_timestamp" json:"create_at"`

	// Khóa ngoại
	IDHotel uuid.UUID `gorm:"type:uuid" json:"id_hotel"`
	IDUser  uuid.UUID `gorm:"type:uuid" json:"id_user"`

	// Quan hệ
	Details []ReportRevenueDetail `gorm:"foreignKey:IDReportRevenue;constraint:OnDelete:CASCADE" json:"details,omitempty"`
}

func (ReportRevenue) TableName() string {
	return "report_revenue"
}
