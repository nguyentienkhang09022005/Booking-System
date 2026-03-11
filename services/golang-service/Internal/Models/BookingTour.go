package Models

import (
	"time"

	"github.com/google/uuid"
)

type BookingTour struct {
	IDBookingTour    uuid.UUID `gorm:"type:uuid;default:uuid_generate_v4();primaryKey" json:"id_booking_tour"`
	QuantityAdult    int       `gorm:"type:int;default:1" json:"quantity_adult"`
	QuantityChild    int       `gorm:"type:int;default:0" json:"quantity_child"`
	PriceBookingTour float64   `gorm:"type:numeric(12,2)" json:"price_booking_tour"`
	PickupLocation   *string   `gorm:"type:varchar(200)" json:"pickup_location,omitempty"`
	LanguageSelected *string   `gorm:"type:varchar(50)" json:"language_selected,omitempty"`
	SpecialRequests  *string   `gorm:"type:text" json:"special_requests,omitempty"`
	CreatedAt        time.Time `gorm:"type:timestamp;default:current_timestamp" json:"created_at"`

	// Khóa ngoại
	IDTour uuid.UUID `gorm:"type:uuid" json:"id_tour"`
	IDUser uuid.UUID `gorm:"type:uuid" json:"id_user"`

	// Quan hệ (Relationships)
	//Tour Tour `gorm:"foreignKey:IDTour;constraint:OnDelete:CASCADE;" json:"tour,omitempty"`
	User User `gorm:"foreignKey:IDUser;constraint:OnDelete:CASCADE;" json:"user,omitempty"`
}

// Chỉ định tên bảng chính xác trong CSDL
func (BookingTour) TableName() string {
	return "booking_tour"
}
