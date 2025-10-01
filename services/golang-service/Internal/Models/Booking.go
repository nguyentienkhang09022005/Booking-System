package Models

import (
	"time"

	"github.com/google/uuid"
)

type Booking struct {
	IDBooking    uuid.UUID `gorm:"type:uuid;default:uuid_generate_v4();primaryKey" json:"id_booking"`
	BookingDate  time.Time `gorm:"type:timestamp;default:current_timestamp" json:"booking_date"`
	CheckinDate  time.Time `json:"checkin_date"`
	CheckoutDate time.Time `json:"checkout_date"`
	Status       string    `gorm:"type:varchar(20)" json:"status"`
	TotalPrice   float64   `gorm:"type:numeric(12,2)" json:"total_price"`

	// Khóa ngoại
	IDUser uuid.UUID `gorm:"type:uuid" json:"id_user"`
	User   User      `gorm:"foreignKey:IDUser;constraint:OnDelete:CASCADE" json:"user,omitempty"`
}
