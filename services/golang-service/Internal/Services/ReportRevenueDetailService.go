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
	UpdateReportRevenueDetail(revenueUpdateEvent Request.RevenueUpdateEvent) error
}

type reportRevenueDetailService struct {
	_reportRevenueRepository       Repositories.IReportRevenueRepository
	_reportRevenueDetailRepository Repositories.IReportRevenueDetailRepository
	_accommodationRepository       Repositories.IAccommodationRepository
}

func ReportRevenueDetailService(
	reportRevenueRepository Repositories.IReportRevenueRepository,
	reportRevenueDetailRepository Repositories.IReportRevenueDetailRepository,
	accommodationRepository Repositories.IAccommodationRepository) IReportRevenueDetailService {
	return &reportRevenueDetailService{
		_reportRevenueRepository:       reportRevenueRepository,
		_reportRevenueDetailRepository: reportRevenueDetailRepository,
		_accommodationRepository:       accommodationRepository,
	}
}

func (r *reportRevenueDetailService) CreateReportRevenueDetail(revenueCreatedEvent Request.RevenueCreatedEvent,
) (*Response.ReportRevenueDetailResponse, error) {
	if _, err := r._accommodationRepository.FindById(revenueCreatedEvent.IDAccommodation); err != nil {
		return nil, Errors.New(http.StatusNotFound, Errors.AccommodationNotFound)
	}

	reportRevenueExisted, err := r._reportRevenueRepository.FindByMonthAndYear(
		revenueCreatedEvent.IDAccommodation,
		revenueCreatedEvent.Month,
		revenueCreatedEvent.Year,
	)

	var reportRevenue *Models.ReportRevenue

	if err != nil || reportRevenueExisted == nil {
		reportRevenue = &Models.ReportRevenue{
			IDReportRevenue: uuid.New(),
			IDAccommodation: revenueCreatedEvent.IDAccommodation,
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

// Hàm thay đổi chi tiết báo cáo doanh thu (có thể bổ sung thêm logic handle booking -> income)
func (r *reportRevenueDetailService) UpdateReportRevenueDetail(revenueUpdateEvent Request.RevenueUpdateEvent) error {
	err := r._reportRevenueDetailRepository.UpdateAmountByExpenseID(
		revenueUpdateEvent.IDExpense,
		revenueUpdateEvent.Amount,
	)

	if err != nil {
		return Errors.New(http.StatusInternalServerError, Errors.InternalError)
	}

	return nil
}
