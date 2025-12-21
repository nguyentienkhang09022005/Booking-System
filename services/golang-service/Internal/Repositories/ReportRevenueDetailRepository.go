package Repositories

import (
	"errors"
	"github.com/google/uuid"
	"gorm.io/gorm"
	"payment-service/Internal/Models"
)

type IReportRevenueDetailRepository interface {
	Create(reportRevenueDetail *Models.ReportRevenueDetail) (*Models.ReportRevenueDetail, error)
	FindById(idReportRevenueDetail uuid.UUID) (*Models.ReportRevenueDetail, error)
	UpdateAmountByExpenseID(idExpense uuid.UUID, newAmount float64) error
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

func (r *reportRevenueDetailRepository) UpdateAmountByExpenseID(idExpense uuid.UUID, newAmount float64) error {
	result := r._db.Model(&Models.ReportRevenueDetail{}).
		Where("id_expense = ?", idExpense).
		Update("amount", newAmount)

	if result.Error != nil {
		return result.Error
	}

	if result.RowsAffected == 0 {
		return errors.New("Không tìm thấy chi tiết chi để cập nhật!")
	}

	return nil
}
