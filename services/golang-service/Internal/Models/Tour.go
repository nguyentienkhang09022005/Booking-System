package Models

import (
	"github.com/google/uuid"
)

type Tour struct {
	IDTour uuid.UUID `gorm:"type:uuid;default:uuid_generate_v4();primaryKey" json:"id_tour"`
	IDUser uuid.UUID `gorm:"type:uuid" json:"id_user"`
}

func (Tour) TableName() string {
	return "tour"
}
