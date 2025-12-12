package Services

import (
	"github.com/google/uuid"
	"net/http"
	"payment-service/Constants"
	"payment-service/Internal/Dtos/Request"
	"payment-service/Internal/Dtos/Response"
	"payment-service/Internal/Errors"
	"payment-service/Internal/Models"
	"payment-service/Internal/Repositories"
)

type IReportRevenueDetailService interface {
	CreateReportRevenueDetail(revenueCreatedEvent Request.RevenueCreatedEvent) (*Response.ReportRevenueDetailResponse, error)
}

type reportRevenueDetailService struct {
	_reportRevenueRepository       Repositories.IReportRevenueRepository
	_reportRevenueDetailRepository Repositories.IReportRevenueDetailRepository
	_hotelRepository               Repositories.IHotelRepository
}

func ReportRevenueDetailService(
	reportRevenueRepository Repositories.IReportRevenueRepository,
	reportRevenueDetailRepository Repositories.IReportRevenueDetailRepository,
	hotelRepository Repositories.IHotelRepository) IReportRevenueDetailService {
	return &reportRevenueDetailService{
		_reportRevenueRepository:       reportRevenueRepository,
		_reportRevenueDetailRepository: reportRevenueDetailRepository,
		_hotelRepository:               hotelRepository,
	}
}

func (r *reportRevenueDetailService) CreateReportRevenueDetail(revenueCreatedEvent Request.RevenueCreatedEvent,
) (*Response.ReportRevenueDetailResponse, error) {
	if _, err := r._hotelRepository.FindById(revenueCreatedEvent.IDHotel); err != nil {
		return nil, Errors.New(http.StatusNotFound, Errors.HotelNotFound)
	}

	reportRevenueExisted, err := r._reportRevenueRepository.FindByMonthAndYear(
		revenueCreatedEvent.IDHotel,
		revenueCreatedEvent.Month,
		revenueCreatedEvent.Year,
	)

	var reportRevenue *Models.ReportRevenue

	if err != nil || reportRevenueExisted == nil {
		reportRevenue = &Models.ReportRevenue{
			IDReportRevenue: uuid.New(),
			IDHotel:         revenueCreatedEvent.IDHotel,
			Month:           revenueCreatedEvent.Month,
			Year:            revenueCreatedEvent.Year,
		}

		reportRevenue, err = r._reportRevenueRepository.Create(reportRevenue)
		if err != nil {
			return nil, Errors.New(http.StatusInternalServerError, Errors.InternalError)
		}
	} else {
		reportRevenue = reportRevenueExisted
	}

	var createdDetail *Models.ReportRevenueDetail

	detail := &Models.ReportRevenueDetail{
		IDRevenueDetail: uuid.New(),
		IDReportRevenue: reportRevenue.IDReportRevenue,
		Amount:          revenueCreatedEvent.Amount,
	}

	if revenueCreatedEvent.IDBooking != nil {
		detail.TypeRevenue = Constants.TypeIncome
		detail.IDBooking = revenueCreatedEvent.IDBooking
		detail.IDExpense = nil
	} else {
		detail.TypeRevenue = Constants.TypeExpense
		detail.IDBooking = nil
		detail.IDExpense = revenueCreatedEvent.IDExpense
	}

	createdDetail, err = r._reportRevenueDetailRepository.Create(detail)
	if err != nil {
		return nil, Errors.New(http.StatusInternalServerError, Errors.InternalError)
	}

	response := &Response.ReportRevenueDetailResponse{
		IDRevenueDetail: createdDetail.IDRevenueDetail,
		TypeRevenue:     createdDetail.TypeRevenue,
		Amount:          createdDetail.Amount,
		CreateAt:        createdDetail.CreateAt,
	}

	return response, nil
}
