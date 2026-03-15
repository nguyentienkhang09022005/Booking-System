package Models

import (
	"time"

	"github.com/google/uuid"
)

type BookingTransport struct {
	IDBookingTransport    uuid.UUID `gorm:"type:uuid;default:uuid_generate_v4();primaryKey" json:"id_booking_transport"`
	QuantityTickets       int       `gorm:"type:int" json:"quantity_tickets"`
	PriceBookingTransport float64   `gorm:"type:numeric(12,2)" json:"price_booking_transport"`
	SeatNumbers           *string   `gorm:"type:varchar(100)" json:"seat_numbers,omitempty"`
	Status                string    `gorm:"type:varchar(20)" json:"status"`
	CreatedAt             time.Time `gorm:"type:timestamp;default:current_timestamp" json:"created_at"`

	// Khóa ngoại
	IDTicket uuid.UUID `gorm:"type:uuid" json:"id_ticket"`
	IDUser   uuid.UUID `gorm:"type:uuid" json:"id_user"`

	// Quan hệ (Relationships)
	// TransportTicket TransportTicket `gorm:"foreignKey:IDTicket;constraint:OnDelete:CASCADE;" json:"transport_ticket,omitempty"`
	User User `gorm:"foreignKey:IDUser;constraint:OnDelete:CASCADE;" json:"user,omitempty"`
}

// Chỉ định tên bảng chính xác trong CSDL
func (BookingTransport) TableName() string {
	return "booking_transport"
}
