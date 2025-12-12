package Response

import (
	"github.com/google/uuid"
	"time"
)

type ReportRoomBookingResponse struct {
	IDReportRoomBooking uuid.UUID                         `json:"id_report_room_booking"`
	Month               int                               `json:"month"`
	Year                int                               `json:"year"`
	CreateAt            time.Time                         `json:"createAt"`
	Details             []ReportRoomBookingDetailResponse `json:"reportDetails,omitempty"`
}
