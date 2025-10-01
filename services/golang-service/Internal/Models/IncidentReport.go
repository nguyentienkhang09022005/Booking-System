package Models

import (
	"time"

	"github.com/google/uuid"
)

type IncidentReport struct {
	IDIncident          uuid.UUID `gorm:"type:uuid;default:uuid_generate_v4();primaryKey" json:"id_incident"`
	IncidentName        string    `gorm:"type:varchar(100)" json:"incident_name"`
	IncidentDescription string    `gorm:"type:text" json:"incident_description"`
	Status              string    `gorm:"type:varchar(20)" json:"status"`
	CreatedAt           time.Time `gorm:"type:timestamp;default:current_timestamp" json:"created_at"`

	// Khóa ngoại
	IDUser    uuid.UUID `gorm:"type:uuid" json:"id_user"`
	IDBooking uuid.UUID `gorm:"type:uuid" json:"id_booking"`

	// Quan hệ
	User    User    `gorm:"foreignKey:IDUser;constraint:OnDelete:CASCADE" json:"user,omitempty"`
	Booking Booking `gorm:"foreignKey:IDBooking;constraint:OnDelete:CASCADE" json:"booking,omitempty"`
}
