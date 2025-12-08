package Models

import (
	"time"

	"github.com/google/uuid"
)

type ReportRevenueDetail struct {
	IDRevenueDetail uuid.UUID `gorm:"type:uuid;default:uuid_generate_v4();primaryKey" json:"id_revenue_detail"`
	TypeRevenue     string    `gorm:"type:varchar(50)" json:"type_revenue"`
	Amount          float64   `gorm:"type:numeric(12,2)" json:"amount"`
	CreateAt        time.Time `gorm:"type:timestamp;default:current_timestamp" json:"create_at"`

	// Khóa ngoại
	IDReportRevenue uuid.UUID  `gorm:"type:uuid" json:"id_report_revenue"`
	IDBooking       *uuid.UUID `gorm:"type:uuid" json:"id_booking"`
	IDExpense       *uuid.UUID `gorm:"type:uuid" json:"id_expense"`

	// Quan hệ
	Booking *Booking       `gorm:"foreignKey:IDBooking;constraint:OnDelete:CASCADE" json:"booking,omitempty"`
	Revenue *ReportRevenue `gorm:"foreignKey:IDReportRevenue;constraint:OnDelete:CASCADE" json:"report_revenue,omitempty"`
}

func (ReportRevenueDetail) TableName() string {
	return "report_revenue_detail"
}
