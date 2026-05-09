package Models

import (
	"github.com/google/uuid"
)

type TransportTicket struct {
	IDTicket uuid.UUID `gorm:"type:uuid;default:uuid_generate_v4();primaryKey" json:"id_ticket"`
	IDUser   uuid.UUID `gorm:"type:uuid" json:"id_user"`
}

func (TransportTicket) TableName() string {
	return "transport_ticket"
}
