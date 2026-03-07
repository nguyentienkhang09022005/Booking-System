package Models

import (
	"github.com/google/uuid"
	"time"
)

type ReportRoomBooking struct {
	IDReportRoomBooking uuid.UUID `gorm:"type:uuid;default:uuid_generate_v4();primaryKey" json:"id_report_room_booking"`
	Month               int       `json:"month"`
	Year                int       `json:"year"`
	CreateAt            time.Time `gorm:"type:timestamp;default:current_timestamp" json:"create_at"`

	// Khóa ngoại
	IDAccommodation uuid.UUID `gorm:"type:uuid" json:"id_accommodation"`

	// Quna hệ
	Details []ReportRoomBookingDetail `gorm:"foreignKey:IDReportRoomBooking;constraint:OnDelete:CASCADE" json:"details,omitempty"`
}

func (ReportRoomBooking) TableName() string { return "report_room_booking" }
