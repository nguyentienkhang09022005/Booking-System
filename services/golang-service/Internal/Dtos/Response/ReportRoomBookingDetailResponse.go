package Response

import (
	"github.com/google/uuid"
)

type ReportRoomBookingDetailResponse struct {
	IDDetail     uuid.UUID `json:"id_detail"`
	TypeRoom     string    `json:"type_room"`
	TotalBooking int       `json:"total_booking"`
}
