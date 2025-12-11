package Models

import (
	"time"

	"github.com/google/uuid"
)

type Booking struct {
	IDBooking    uuid.UUID `gorm:"type:uuid;default:uuid_generate_v4();primaryKey" json:"id_booking"`
	BookingDate  time.Time `gorm:"type:timestamp;default:current_timestamp" json:"booking_date"`
	CheckinDate  time.Time `gorm:"type:timestamp" json:"checkin_date"`
	CheckoutDate time.Time `gorm:"type:timestamp" json:"checkout_date"`
	Note         string    `gorm:"type:text" json:"note"`
	Status       string    `gorm:"type:varchar(20)" json:"status"`
	TotalPrice   float64   `gorm:"type:numeric(12,2)" json:"total_price"`

	// Khóa ngoại
	IDUser  uuid.UUID `gorm:"type:uuid" json:"id_user"`
	IDHotel uuid.UUID `gorm:"type:uuid" json:"id_hotel"`

	// Quan hệ
	User  User  `gorm:"foreignKey:IDUser;constraint:OnDelete:CASCADE" json:"user,omitempty"`
	Hotel Hotel `gorm:"foreignKey:IDHotel;constraint:OnDelete:CASCADE" json:"hotel,omitempty"`
}

func (Booking) TableName() string {
	return "booking"
}
