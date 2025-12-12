package Services

import (
	"github.com/google/uuid"
	"net/http"
	"payment-service/Internal/Dtos/Response"
	"payment-service/Internal/Errors"
	"payment-service/Internal/Models"
	"payment-service/Internal/Repositories"
)

type IReportRoomBookingService interface {
	GetInfReportRoomBookingByMonth(idHotel uuid.UUID, month int, year int) (*Response.ReportRoomBookingResponse, error)
	DeleteReportRoomBooking(idReportRoomBooking uuid.UUID) error
}

type reportRoomBookingService struct {
	_reportRoomBookingRepository Repositories.IReportRoomBookingRepository
	_hotelRepository             Repositories.IHotelRepository
}

func ReportRoomBookingService(reportRoomBookingRepository Repositories.IReportRoomBookingRepository,
	hotelRepository Repositories.IHotelRepository) IReportRoomBookingService {
	return &reportRoomBookingService{
		_reportRoomBookingRepository: reportRoomBookingRepository,
		_hotelRepository:             hotelRepository,
	}
}

func (r *reportRoomBookingService) GetInfReportRoomBookingByMonth(idHotel uuid.UUID, month int, year int,
) (*Response.ReportRoomBookingResponse, error) {

	_, err := r._hotelRepository.FindById(idHotel)
	if err != nil {
		return nil, Errors.New(http.StatusNotFound, Errors.HotelNotFound)
	}

	report, err := r._reportRoomBookingRepository.FindByMonthAndYear(idHotel, month, year)
	if err != nil {
		return nil, Errors.New(http.StatusInternalServerError, Errors.InternalError)
	}

	if report == nil {
		report = &Models.ReportRoomBooking{
			IDReportRoomBooking: uuid.New(),
			IDHotel:             idHotel,
			Month:               month,
			Year:                year,
		}

		report, err = r._reportRoomBookingRepository.Create(report)
		if err != nil {
			return nil, Errors.New(http.StatusInternalServerError, Errors.InternalError)
		}
	}

	typeRooms, err := r._hotelRepository.FindTypeRoomsByHotel(idHotel)
	if err != nil {
		return nil, Errors.New(http.StatusInternalServerError, Errors.InternalError)
	}

	// Map detail cũ (nếu có)
	detailMap := make(map[string]*Models.ReportRoomBookingDetail)
	for _, detail := range report.Details {
		detailMap[detail.TypeRoom] = &detail
	}

	finalDetails := make([]Models.ReportRoomBookingDetail, 0)

	// Duyệt từng TypeRoom và tính tổng booking
	for _, tr := range typeRooms {

		totalBooking, err := r._reportRoomBookingRepository.CountBookingByTypeRoom(idHotel, tr.TypeRoom, month, year)
		if err != nil {
			return nil, Errors.New(http.StatusInternalServerError, Errors.InternalError)
		}

		// Nếu đã tồn tại detail
		if old, exists := detailMap[tr.TypeRoom]; exists {
			old.TotalBooking = totalBooking
			finalDetails = append(finalDetails, *old)

		} else {
			// Nếu chưa có
			finalDetails = append(finalDetails, Models.ReportRoomBookingDetail{
				IDDetail:            uuid.New(),
				TypeRoom:            tr.TypeRoom,
				TotalBooking:        totalBooking,
				IDReportRoomBooking: report.IDReportRoomBooking,
			})
		}
	}

	report.Details = finalDetails

	if len(finalDetails) > 0 {
		err = r._reportRoomBookingRepository.SaveDetails(finalDetails)
		if err != nil {
			return nil, Errors.New(http.StatusInternalServerError, Errors.InternalError)
		}
	}

	response := &Response.ReportRoomBookingResponse{
		IDReportRoomBooking: report.IDReportRoomBooking,
		Month:               report.Month,
		Year:                report.Year,
		CreateAt:            report.CreateAt,
	}

	for _, d := range finalDetails {
		response.Details = append(response.Details, Response.ReportRoomBookingDetailResponse{
			IDDetail:     d.IDDetail,
			TypeRoom:     d.TypeRoom,
			TotalBooking: d.TotalBooking,
		})
	}
	return response, nil
}

func (r *reportRoomBookingService) DeleteReportRoomBooking(idReportRoomBooking uuid.UUID) error {
	_, err := r._reportRoomBookingRepository.FindById(idReportRoomBooking)
	if err != nil {
		return Errors.New(http.StatusNotFound, Errors.ReportRoomBookingNotFound)
	}

	return r._reportRoomBookingRepository.Delete(idReportRoomBooking)
}
