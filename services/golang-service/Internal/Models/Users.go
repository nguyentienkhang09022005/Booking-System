package Models

import (
	"time"

	"github.com/google/uuid"
)

type User struct {
	IDUser      uuid.UUID `gorm:"type:uuid;default:uuid_generate_v4();primaryKey" json:"id_user"`
	RoleName    string    `gorm:"type:varchar(20)" json:"role_name"`
	TypeUser    string    `gorm:"type:varchar(30)" json:"type_user"`
	FirstName   string    `gorm:"type:varchar(20)" json:"first_name"`
	LastName    string    `gorm:"type:varchar(20)" json:"last_name"`
	Phone       string    `gorm:"type:varchar(12);unique;check:length(phone) between 10 and 12" json:"phone"`
	Gender      string    `gorm:"type:varchar(5);check:gender in ('Nam','Nữ')" json:"gender"`
	DOB         time.Time `json:"dob"`
	AddressUser string    `gorm:"type:text" json:"address_user"`
	Email       string    `gorm:"type:text;unique" json:"email"`
	Password    string    `gorm:"column:password_user;type:text" json:"password_user"`
	CreatedAt   time.Time `gorm:"type:timestamp;default:current_timestamp" json:"created_at"`

	// Quan hệ
	Bookings []Booking `gorm:"foreignKey:IDUser;constraint:OnDelete:CASCADE" json:"bookings,omitempty"`
}
