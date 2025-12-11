package Services

import (
	"github.com/google/uuid"
	"net/http"
	"payment-service/Internal/Dtos/Request"
	"payment-service/Internal/Dtos/Response"
	"payment-service/Internal/Errors"
	"payment-service/Internal/Models"
	"payment-service/Internal/Repositories"
)

type IReportRevenueService interface {
	CreateReportRevenue(reportRevenueRequest Request.ReportRevenueRequest) (*Response.ReportRevenueResponse, error)
	DeleteReportRevenue(idReportRevenue uuid.UUID) error
}

type reportRevenueService struct {
	_reportRevenueRepository Repositories.IReportRevenueRepository
	_hotelRepository         Repositories.IHotelRepository
	_userRepository          Repositories.IUserRepository
}

func ReportRevenueService(reportRevenueRepository Repositories.IReportRevenueRepository,
	hotelRepository Repositories.IHotelRepository,
	userRepository Repositories.IUserRepository) IReportRevenueService {
	return &reportRevenueService{
		_reportRevenueRepository: reportRevenueRepository,
		_hotelRepository:         hotelRepository,
		_userRepository:          userRepository,
	}
}

func (r *reportRevenueService) CreateReportRevenue(reportRevenueRequest Request.ReportRevenueRequest) (*Response.ReportRevenueResponse, error) {
	_, err := r._hotelRepository.FindById(reportRevenueRequest.IDHotel)
	if err != nil {
		return nil, Errors.New(http.StatusNotFound, Errors.HotelNotFound)
	}

	newReportRevenue := &Models.ReportRevenue{
		IDReportRevenue: uuid.New(),
		IDHotel:         reportRevenueRequest.IDHotel,
		Month:           reportRevenueRequest.Month,
		Year:            reportRevenueRequest.Year,
	}

	createdReportRevenue, err := r._reportRevenueRepository.Create(newReportRevenue)
	if err != nil {
		return nil, Errors.New(http.StatusInternalServerError, Errors.InternalError)
	}

	response := &Response.ReportRevenueResponse{
		IDReportRevenue: createdReportRevenue.IDReportRevenue,
		Month:           createdReportRevenue.Month,
		Year:            createdReportRevenue.Year,
		TotalRevenue:    createdReportRevenue.TotalRevenue,
		CreateAt:        createdReportRevenue.CreateAt,
	}
	return response, nil
}

func (r *reportRevenueService) DeleteReportRevenue(idReportRevenue uuid.UUID) error {
	_, err := r._reportRevenueRepository.FindById(idReportRevenue)
	if err != nil {
		return Errors.New(http.StatusNotFound, Errors.ReportRevenueNotFound)
	}

	return r._reportRevenueRepository.Delete(idReportRevenue)
}
