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
	GetInfReportByMonth(idHotel uuid.UUID, month int, year int) (*Response.ReportRevenueResponse, error)
	CreateReportRevenue(reportRevenueRequest Request.ReportRevenueRequest) (*Response.ReportRevenueResponse, error)
	DeleteReportRevenue(idReportRevenue uuid.UUID) error
}

type reportRevenueService struct {
	_reportRevenueRepository Repositories.IReportRevenueRepository
	_hotelRepository         Repositories.IHotelRepository
}

func ReportRevenueService(reportRevenueRepository Repositories.IReportRevenueRepository,
	hotelRepository Repositories.IHotelRepository) IReportRevenueService {
	return &reportRevenueService{
		_reportRevenueRepository: reportRevenueRepository,
		_hotelRepository:         hotelRepository,
	}
}

func (r *reportRevenueService) GetInfReportByMonth(idHotel uuid.UUID, month int, year int) (*Response.ReportRevenueResponse, error) {
	_, err := r._hotelRepository.FindById(idHotel)
	if err != nil {
		return nil, Errors.New(http.StatusNotFound, Errors.HotelNotFound)
	}

	reportRevenueExisted, err := r._reportRevenueRepository.FindByMonthAndYear(idHotel, month, year)

	if err != nil {
		return nil, Errors.New(http.StatusNotFound, Errors.ReportRevenueNotFound)
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
	_, err := r._hotelRepository.FindById(reportRevenueRequest.IDHotel)
	if err != nil {
		return nil, Errors.New(http.StatusNotFound, Errors.HotelNotFound)
	}

	reportRevenueExisted, err := r._reportRevenueRepository.FindByMonthAndYear(
		reportRevenueRequest.IDHotel,
		reportRevenueRequest.Month,
		reportRevenueRequest.Year,
	)

	if err != nil || reportRevenueExisted != nil {
		return nil, Errors.New(http.StatusConflict, Errors.RevenueExists)
	}

	newIdReportRevenue := uuid.New()
	newReportRevenue := &Models.ReportRevenue{
		IDReportRevenue: newIdReportRevenue,
		IDHotel:         reportRevenueRequest.IDHotel,
		Month:           reportRevenueRequest.Month,
		Year:            reportRevenueRequest.Year,
	}

	// Quét dữ liệu từ booking và expense
	bookings, err := r._reportRevenueRepository.GetBookingsByMonth(reportRevenueRequest.IDHotel, reportRevenueRequest.Month, reportRevenueRequest.Year)
	if err != nil {
		return nil, Errors.New(http.StatusInternalServerError, Errors.InternalError)
	}

	expenses, err := r._reportRevenueRepository.GetExpensesByMonth(reportRevenueRequest.IDHotel, reportRevenueRequest.Month, reportRevenueRequest.Year)
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
