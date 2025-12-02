package Services

import (
	"fmt"
	"github.com/google/uuid"
	"payment-service/Internal/Dtos/Request"
	"payment-service/Internal/Dtos/Response"
	"payment-service/Internal/Models"
	"payment-service/Internal/Repositories"
)

type IReportRevenueService interface {
	CreateReportRevenue(reportRevenueRequest Request.ReportRevenueRequest) (*Response.ReportRevenueResponse, error)
	DeleteReportRevenue(idReportRevenue string) error
}

type reportRevenueService struct {
	_reportRevenueRepository Repositories.IReportRevenueRepository
}

func ReportRevenueService(reportRevenueRepository Repositories.IReportRevenueRepository) IReportRevenueService {
	return &reportRevenueService{_reportRevenueRepository: reportRevenueRepository}
}

func (r *reportRevenueService) CreateReportRevenue(reportRevenueRequest Request.ReportRevenueRequest) (*Response.ReportRevenueResponse, error) {
	newReportRevenue := &Models.ReportRevenue{
		IDReportRevenue: uuid.New(),
		IDUser:          reportRevenueRequest.IDUser,
		IDHotel:         reportRevenueRequest.IDHotel,
		Month:           reportRevenueRequest.Month,
		Year:            reportRevenueRequest.Year,
	}

	createdReportRevenue, err := r._reportRevenueRepository.Create(newReportRevenue)
	if err != nil {
		return nil, fmt.Errorf("Không thể tạo báo cáo thu chi!: %w", err)
	}

	// Response
	response := &Response.ReportRevenueResponse{
		IDReportRevenue: createdReportRevenue.IDReportRevenue,
		Month:           createdReportRevenue.Month,
		Year:            createdReportRevenue.Year,
		TotalRevenue:    createdReportRevenue.TotalRevenue,
		CreateAt:        createdReportRevenue.CreateAt,
	}
	return response, nil
}

func (r *reportRevenueService) DeleteReportRevenue(idReportRevenue string) error {
	return r._reportRevenueRepository.Delete(idReportRevenue)
}
