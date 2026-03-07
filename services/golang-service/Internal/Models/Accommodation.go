package Models

import (
	"time"

	"github.com/google/uuid"
)

type Accommodation struct {
	IDAccommodation   uuid.UUID `gorm:"type:uuid;default:uuid_generate_v4();primaryKey" json:"id_accommodation"`
	AccommodationName string    `gorm:"type:varchar(100)" json:"Accommodation_name"`
	Street            string    `gorm:"type:varchar(100)" json:"street"`
	Ward              string    `gorm:"type:varchar(30)" json:"ward"`
	City              string    `gorm:"type:varchar(30)" json:"city"`
	Description       string    `gorm:"type:text" json:"description"`
	IsApproved        bool      `gorm:"default:false" json:"is_approved"`
	CreatedAt         time.Time `gorm:"type:timestamp;default:current_timestamp" json:"created_at"`

	// Khóa ngoại
	IDUser uuid.UUID `gorm:"type:uuid" json:"id_user"`

	// Quan hệ
	User User `gorm:"foreignKey:IDUser;constraint:OnDelete:CASCADE" json:"user,omitempty"`
}

func (Accommodation) TableName() string {
	return "accommodation"
}
