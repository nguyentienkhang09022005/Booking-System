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

type IReportRevenueService interface {
	GetInfReportByMonth(idAccommodation uuid.UUID, month int, year int) (*Response.ReportRevenueResponse, error)
	CreateReportRevenue(reportRevenueRequest Request.ReportRevenueRequest) (*Response.ReportRevenueResponse, error)
	DeleteReportRevenue(idReportRevenue uuid.UUID) error
	GetYearlyRevenueStats(idAccommodation uuid.UUID, year int) ([]Response.MonthlyRevenueStatResponse, error)
}

type reportRevenueService struct {
	_reportRevenueRepository Repositories.IReportRevenueRepository
	_accommodationRepository Repositories.IAccommodationRepository
}

func ReportRevenueService(reportRevenueRepository Repositories.IReportRevenueRepository,
	accommodationRepository Repositories.IAccommodationRepository) IReportRevenueService {
	return &reportRevenueService{
		_reportRevenueRepository: reportRevenueRepository,
		_accommodationRepository: accommodationRepository,
	}
}

func (r *reportRevenueService) GetInfReportByMonth(idAccommodation uuid.UUID, month int, year int) (*Response.ReportRevenueResponse, error) {
	_, err := r._accommodationRepository.FindById(idAccommodation)
	if err != nil {
		return nil, Errors.New(http.StatusNotFound, Errors.AccommodationNotFound)
	}

	reportRevenueExisted, err := r._reportRevenueRepository.FindByMonthAndYear(idAccommodation, month, year)

	if err != nil || reportRevenueExisted == nil {
		return &Response.ReportRevenueResponse{
			IDReportRevenue: uuid.Nil,
			Month:           month,
			Year:            year,
			TotalRevenue:    0,
			TotalIncome:     0,
			TotalExpense:    0,
			Details:         []Response.ReportRevenueDetailResponse{},
		}, nil
	}

	var totalIncome float64 = 0
	var totalExpense float64 = 0
	var detailsResponse []Response.ReportRevenueDetailResponse

	for _, detail := range reportRevenueExisted.Details {
		detailsResponse = append(detailsResponse, Response.ReportRevenueDetailResponse{
			IDRevenueDetail: detail.IDRevenueDetail,
			TypeRevenue:     detail.TypeRevenue,
			Amount:          detail.Amount,
			CreateAt:        detail.CreateAt,
		})

		if detail.TypeRevenue == Constants.TypeIncome {
			totalIncome += detail.Amount
		} else if detail.TypeRevenue == Constants.TypeExpense {
			totalExpense += detail.Amount
		}
	}

	response := &Response.ReportRevenueResponse{
		IDReportRevenue: reportRevenueExisted.IDReportRevenue,
		Month:           reportRevenueExisted.Month,
		Year:            reportRevenueExisted.Year,
		TotalRevenue:    totalIncome - totalExpense,
		TotalIncome:     totalIncome,
		TotalExpense:    totalExpense,
		CreateAt:        reportRevenueExisted.CreateAt,
		Details:         detailsResponse,
	}
	return response, nil
}

func (r *reportRevenueService) CreateReportRevenue(reportRevenueRequest Request.ReportRevenueRequest) (*Response.ReportRevenueResponse, error) {
	_, err := r._accommodationRepository.FindById(reportRevenueRequest.IDAccommodation)
	if err != nil {
		return nil, Errors.New(http.StatusNotFound, Errors.AccommodationNotFound)
	}

	reportRevenueExisted, err := r._reportRevenueRepository.FindByMonthAndYear(
		reportRevenueRequest.IDAccommodation,
		reportRevenueRequest.Month,
		reportRevenueRequest.Year,
	)

	if err != nil || reportRevenueExisted != nil {
		return nil, Errors.New(http.StatusConflict, Errors.RevenueExists)
	}

	newIdReportRevenue := uuid.New()
	newReportRevenue := &Models.ReportRevenue{
		IDReportRevenue: newIdReportRevenue,
		IDAccommodation: reportRevenueRequest.IDAccommodation,
		Month:           reportRevenueRequest.Month,
		Year:            reportRevenueRequest.Year,
	}

	// Quét dữ liệu từ booking và expense
	bookings, err := r._reportRevenueRepository.GetBookingsByMonth(reportRevenueRequest.IDAccommodation, reportRevenueRequest.Month, reportRevenueRequest.Year)
	if err != nil {
		return nil, Errors.New(http.StatusInternalServerError, Errors.InternalError)
	}

	expenses, err := r._reportRevenueRepository.GetExpensesByMonth(reportRevenueRequest.IDAccommodation, reportRevenueRequest.Month, reportRevenueRequest.Year)
	if err != nil {
		return nil, Errors.New(http.StatusInternalServerError, Errors.InternalError)
	}

	var details []Models.ReportRevenueDetail
	var totalRevenue float64 = 0

	for _, b := range bookings {
		currentBookingID := b.IDBooking

		detail := Models.ReportRevenueDetail{
			IDRevenueDetail: uuid.New(),
			TypeRevenue:     Constants.TypeIncome,
			Amount:          b.TotalPrice,
			IDReportRevenue: newIdReportRevenue,
			IDBooking:       &currentBookingID,
			IDExpense:       nil,
		}
		details = append(details, detail)
		totalRevenue += b.TotalPrice
	}

	for _, e := range expenses {
		currentExpenseID := e.IDExpense

		detail := Models.ReportRevenueDetail{
			IDRevenueDetail: uuid.New(),
			TypeRevenue:     Constants.TypeExpense,
			Amount:          e.Amount,
			IDReportRevenue: newIdReportRevenue,
			IDBooking:       nil,
			IDExpense:       &currentExpenseID,
		}
		details = append(details, detail)
		totalRevenue -= e.Amount
	}

	newReportRevenue.TotalRevenue = totalRevenue

	createdReportRevenue, err := r._reportRevenueRepository.Create(newReportRevenue)
	if err != nil {
		return nil, Errors.New(http.StatusInternalServerError, Errors.InternalError)
	}

	if len(details) > 0 {
		err = r._reportRevenueRepository.CreateDetails(details)
		if err != nil {
			return nil, Errors.New(http.StatusInternalServerError, Errors.InternalError)
		}
	}

	response := &Response.ReportRevenueResponse{
		IDReportRevenue: createdReportRevenue.IDReportRevenue,
		Month:           createdReportRevenue.Month,
		Year:            createdReportRevenue.Year,
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

func (r *reportRevenueService) GetYearlyRevenueStats(idAccommodation uuid.UUID, year int) ([]Response.MonthlyRevenueStatResponse, error) {
	_, err := r._accommodationRepository.FindById(idAccommodation)
	if err != nil {
		return nil, Errors.New(http.StatusNotFound, Errors.AccommodationNotFound)
	}

	rawStats, err := r._reportRevenueRepository.GetYearlyStats(idAccommodation, year)
	if err != nil {
		return nil, Errors.New(http.StatusInternalServerError, Errors.InternalError)
	}

	finalStats := make([]Response.MonthlyRevenueStatResponse, 12)
	for i := 0; i < 12; i++ {
		finalStats[i] = Response.MonthlyRevenueStatResponse{
			Month:        i + 1,
			TotalIncome:  0,
			TotalExpense: 0,
		}
	}

	for _, stat := range rawStats {
		if stat.Month >= 1 && stat.Month <= 12 {
			finalStats[stat.Month-1].TotalIncome = stat.TotalIncome
			finalStats[stat.Month-1].TotalExpense = stat.TotalExpense
		}
	}

	return finalStats, nil
}
