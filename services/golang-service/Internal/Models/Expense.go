package Models

import (
	"time"

	"github.com/google/uuid"
)

type Expense struct {
	IDExpense    uuid.UUID `gorm:"type:uuid;default:uuid_generate_v4();primaryKey" json:"id_expense"`
	ExpenseTitle string    `gorm:"type:varchar(150)" json:"expense_title"`
	Amount       float64   `gorm:"type:numeric(12,2)" json:"amount"`
	Description  string    `gorm:"type:text" json:"description"`
	Department   string    `gorm:"type:varchar(50)" json:"department"`
	CreateAt     time.Time `gorm:"type:timestamp;default:current_timestamp" json:"create_at"`

	// Khóa ngoại
	IDUser          uuid.UUID `gorm:"type:uuid" json:"id_user"`
	IDAccommodation uuid.UUID `gorm:"type:uuid" json:"id_accommodation"`

	// Quan hệ
	User          User          `gorm:"foreignKey:IDUser;constraint:OnDelete:CASCADE" json:"user,omitempty"`
	Accommodation Accommodation `gorm:"foreignKey:IDAccommodation;constraint:OnDelete:CASCADE" json:"accommodation,omitempty"`
}

func (Expense) TableName() string {
	return "expense"
}
