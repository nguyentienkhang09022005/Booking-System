package Models

import (
	"time"

	"github.com/google/uuid"
)

type Payment struct {
	IDPayment  uuid.UUID `gorm:"type:uuid;default:uuid_generate_v4();primaryKey" json:"id_payment"`
	Amount     float64   `gorm:"type:numeric(12,2)" json:"amount"`
	Method     string    `gorm:"type:varchar(20)" json:"method"`
	Status     string    `gorm:"type:varchar(20)" json:"status"`
	IDAppTrans string    `gorm:"type:varchar(100)" json:"id_app_trans"`
	IDZpTrans  string    `gorm:"type:varchar(100)" json:"id_zp_trans"`
	CreateAt   time.Time `gorm:"type:timestamp;default:current_timestamp" json:"create_at"`

	// Khóa ngoại
	IDUser    uuid.UUID `gorm:"type:uuid" json:"id_user"`
	IDBooking uuid.UUID `gorm:"type:uuid" json:"id_booking"`

	// Quan hệ
	User    User    `gorm:"foreignKey:IDUser;constraint:OnDelete:CASCADE;" json:"user,omitempty"`
	Booking Booking `gorm:"foreignKey:IDBooking;constraint:OnDelete:CASCADE;" json:"booking,omitempty"`
}

func (Payment) TableName() string {
	return "payment"
}
