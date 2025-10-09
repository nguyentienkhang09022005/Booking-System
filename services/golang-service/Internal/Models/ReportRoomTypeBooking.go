package Models

import (
	"github.com/google/uuid"
)

type ReportRoomTypeBooking struct {
	IDReportTypeBooking uuid.UUID `gorm:"type:uuid;default:uuid_generate_v4();primaryKey" json:"id_report_type_booking"`
	Month               int       `json:"month"`
	Year                int       `json:"year"`
	TotalBooking        int       `json:"total_booking"`

	// Khóa ngoại
	IDTypeRoom uuid.UUID `gorm:"type:varchar(30)" json:"type_room"`
	IDUser     uuid.UUID `gorm:"type:uuid" json:"id_user"`
	IDHotel    uuid.UUID `gorm:"type:uuid" json:"id_hotel"`
}
