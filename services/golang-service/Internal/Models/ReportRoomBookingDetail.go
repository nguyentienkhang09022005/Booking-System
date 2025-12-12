package Models

import (
	"github.com/google/uuid"
)

type ReportRoomBookingDetail struct {
	IDDetail     uuid.UUID `gorm:"type:uuid;default:uuid_generate_v4();primaryKey" json:"id_detail"`
	TypeRoom     string    `gorm:"type:varchar(30)" json:"type_room"`
	TotalBooking int       `json:"total_booking"`

	// Khóa ngoại
	IDReportRoomBooking uuid.UUID `gorm:"type:uuid" json:"id_report_room_booking"`

	// Quan hệ
	Report   *ReportRoomBooking `gorm:"foreignKey:IDReportRoomBooking;constraint:OnDelete:CASCADE" json:"report_room_booking,omitempty"`
	RoomType *TypeRoom          `gorm:"foreignKey:TypeRoom;references:TypeRoom;constraint:OnDelete:CASCADE" json:"room_type,omitempty"`
}

func (ReportRoomBookingDetail) TableName() string { return "report_room_booking_detail" }
