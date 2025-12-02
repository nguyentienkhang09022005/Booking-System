package Repositories

import (
	"gorm.io/gorm"
	"payment-service/Internal/Models"
)

type IReportRevenueRepository interface {
	Create(reportRevenue *Models.ReportRevenue) (*Models.ReportRevenue, error)
	Delete(idReportRevenue string) error
}
type reportRevenueRepository struct {
	_db *gorm.DB
}

// Construtor
func ReportRevenueRepository(db *gorm.DB) IReportRevenueRepository {
	return &reportRevenueRepository{_db: db}
}

func (r *reportRevenueRepository) Create(reportRevenue *Models.ReportRevenue) (*Models.ReportRevenue, error) {
	err := r._db.Create(reportRevenue).Error
	if err != nil {
		return nil, err
	}
	return reportRevenue, nil
}

func (r *reportRevenueRepository) Delete(idReportRevenue string) error {
	result := r._db.Delete(&Models.ReportRevenue{}, "id_report_revenue = ?", idReportRevenue)
	if result.Error != nil {
		return result.Error
	}

	if result.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}
	return nil
}
