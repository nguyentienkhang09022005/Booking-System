package Repositories

import (
	"github.com/google/uuid"
	"gorm.io/gorm"
	"payment-service/Internal/Models"
)

type IReportRevenueDetailRepository interface {
	Create(reportRevenueDetail *Models.ReportRevenueDetail) (*Models.ReportRevenueDetail, error)
	Delete(idReportRevenueDetail uuid.UUID) error
	FindById(idReportRevenueDetail uuid.UUID) (*Models.ReportRevenueDetail, error)
}
type reportRevenueDetailRepository struct {
	_db *gorm.DB
}

// Construtor
func ReportRevenueDetailRepository(db *gorm.DB) IReportRevenueDetailRepository {
	return &reportRevenueDetailRepository{_db: db}
}

func (r *reportRevenueDetailRepository) FindById(idReportRevenueDetail uuid.UUID) (*Models.ReportRevenueDetail, error) {
	var reportRevenueDetail Models.ReportRevenueDetail

	if err := r._db.Where("id_revenue_detail = ?", idReportRevenueDetail).First(&reportRevenueDetail).Error; err != nil {
		return nil, err
	}
	return &reportRevenueDetail, nil
}

func (r *reportRevenueDetailRepository) Create(reportRevenueDetail *Models.ReportRevenueDetail) (*Models.ReportRevenueDetail, error) {
	err := r._db.Create(reportRevenueDetail).Error
	if err != nil {
		return nil, err
	}
	return reportRevenueDetail, nil
}

func (r *reportRevenueDetailRepository) Delete(idReportRevenueDetail uuid.UUID) error {
	result := r._db.Delete(&Models.ReportRevenueDetail{}, "id_revenue_detail = ?", idReportRevenueDetail)
	if result.Error != nil {
		return result.Error
	}

	if result.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}
	return nil
}
