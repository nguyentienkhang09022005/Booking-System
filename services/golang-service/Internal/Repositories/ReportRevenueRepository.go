package Repositories

import (
	"errors"
	"github.com/google/uuid"
	"gorm.io/gorm"
	"payment-service/Internal/Models"
)

type IReportRevenueRepository interface {
	Create(reportRevenue *Models.ReportRevenue) (*Models.ReportRevenue, error)
	Delete(idReportRevenue uuid.UUID) error
	FindById(idReportRevenue uuid.UUID) (*Models.ReportRevenue, error)
	FindByMonthAndYear(idHotel uuid.UUID, month int, year int) (*Models.ReportRevenue, error)
	GetBookingsByMonth(idHotel uuid.UUID, month int, year int) ([]Models.Booking, error)
	GetExpensesByMonth(idHotel uuid.UUID, month int, year int) ([]Models.Expense, error)
	CreateDetails(details []Models.ReportRevenueDetail) error
}
type reportRevenueRepository struct {
	_db *gorm.DB
}

// Construtor
func ReportRevenueRepository(db *gorm.DB) IReportRevenueRepository {
	return &reportRevenueRepository{_db: db}
}

func (r *reportRevenueRepository) FindById(idReportRevenue uuid.UUID) (*Models.ReportRevenue, error) {
	var reportRevenue Models.ReportRevenue

	if err := r._db.Where("id_report_revenue = ?", idReportRevenue).First(&reportRevenue).Error; err != nil {
		return nil, err
	}
	return &reportRevenue, nil
}

func (r *reportRevenueRepository) FindByMonthAndYear(idHotel uuid.UUID, month int, year int) (*Models.ReportRevenue, error) {
	var reportRevenue Models.ReportRevenue

	err := r._db.
		Preload("Details").
		Where("id_hotel = ? AND month = ? AND year = ?", idHotel, month, year).
		First(&reportRevenue).Error

	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, nil
		}
		return nil, err
	}
	return &reportRevenue, nil
}

func (r *reportRevenueRepository) Create(reportRevenue *Models.ReportRevenue) (*Models.ReportRevenue, error) {
	err := r._db.Create(reportRevenue).Error
	if err != nil {
		return nil, err
	}
	return reportRevenue, nil
}

func (r *reportRevenueRepository) Delete(idReportRevenue uuid.UUID) error {
	result := r._db.Delete(&Models.ReportRevenue{}, "id_report_revenue = ?", idReportRevenue)
	if result.Error != nil {
		return result.Error
	}

	if result.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}
	return nil
}

func (r *reportRevenueRepository) GetBookingsByMonth(idHotel uuid.UUID, month int, year int) ([]Models.Booking, error) {
	var bookings []Models.Booking
	err := r._db.Where("id_hotel = ? AND status = ? AND EXTRACT(MONTH FROM booking_date) = ? AND EXTRACT(YEAR FROM booking_date) = ?", idHotel, "Success", month, year).
		Find(&bookings).Error
	return bookings, err
}

func (r *reportRevenueRepository) GetExpensesByMonth(idHotel uuid.UUID, month int, year int) ([]Models.Expense, error) {
	var expenses []Models.Expense
	err := r._db.Where("id_hotel = ? AND EXTRACT(MONTH FROM create_at) = ? AND EXTRACT(YEAR FROM create_at) = ?", idHotel, month, year).
		Find(&expenses).Error
	return expenses, err
}

func (r *reportRevenueRepository) CreateDetails(details []Models.ReportRevenueDetail) error {
	if len(details) == 0 {
		return nil
	}
	return r._db.Create(&details).Error
}
