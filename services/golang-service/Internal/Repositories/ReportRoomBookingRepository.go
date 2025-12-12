package Repositories

import (
	"errors"
	"github.com/google/uuid"
	"gorm.io/gorm"
	"payment-service/Internal/Models"
)

type IReportRoomBookingRepository interface {
	Create(reportRoomBooking *Models.ReportRoomBooking) (*Models.ReportRoomBooking, error)
	Delete(idReportRoomBooking uuid.UUID) error
	FindById(idReportRoomBooking uuid.UUID) (*Models.ReportRoomBooking, error)
	FindByMonthAndYear(idHotel uuid.UUID, month int, year int) (*Models.ReportRoomBooking, error)
	CountBookingByTypeRoom(idHotel uuid.UUID, typeRoom string, month int, year int) (int, error)
	SaveDetails(details []Models.ReportRoomBookingDetail) error
}
type reportRoomBookingRepository struct {
	_db *gorm.DB
}

// Construtor
func ReportRoomBookingRepository(db *gorm.DB) IReportRoomBookingRepository {
	return &reportRoomBookingRepository{_db: db}
}

func (r *reportRoomBookingRepository) FindById(idReportRoomBooking uuid.UUID) (*Models.ReportRoomBooking, error) {
	var reportRoomBooking Models.ReportRoomBooking

	if err := r._db.Where("id_report_room_booking = ?", idReportRoomBooking).First(&reportRoomBooking).Error; err != nil {
		return nil, err
	}
	return &reportRoomBooking, nil
}

func (r *reportRoomBookingRepository) FindByMonthAndYear(idHotel uuid.UUID, month int, year int) (*Models.ReportRoomBooking, error) {
	var reportRoomBooking Models.ReportRoomBooking

	err := r._db.
		Preload("Details").
		Where("id_hotel = ? AND month = ? AND year = ?", idHotel, month, year).
		First(&reportRoomBooking).Error

	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, nil
		}
		return nil, err
	}
	return &reportRoomBooking, nil
}

func (r *reportRoomBookingRepository) Create(reportRoomBooking *Models.ReportRoomBooking) (*Models.ReportRoomBooking, error) {
	err := r._db.Create(reportRoomBooking).Error
	if err != nil {
		return nil, err
	}
	return reportRoomBooking, nil
}

func (r *reportRoomBookingRepository) Delete(idReportRoomBooking uuid.UUID) error {
	result := r._db.Delete(&Models.ReportRoomBooking{}, "id_report_room_booking = ?", idReportRoomBooking)
	if result.Error != nil {
		return result.Error
	}

	if result.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}
	return nil
}

func (r *reportRoomBookingRepository) CountBookingByTypeRoom(idHotel uuid.UUID, typeRoom string, month int, year int) (int, error) {
	var totalQuantity int64
	err := r._db.Table("booking_room").
		Select("COALESCE(SUM(booking_room.quantity), 0)").
		Joins("JOIN booking ON booking.id_booking = booking_room.id_booking").
		Joins("JOIN room ON room.id_room = booking_room.id_room").
		Where("booking.id_hotel = ?", idHotel).
		Where("room.type_room = ?", typeRoom).
		Where("EXTRACT(MONTH FROM booking.checkin_date) = ?", month).
		Where("EXTRACT(YEAR FROM booking.checkin_date) = ?", year).
		Where("booking.status != ?", "Cancelled").
		Scan(&totalQuantity).Error

	if err != nil {
		return 0, err
	}

	return int(totalQuantity), nil
}

func (r *reportRoomBookingRepository) SaveDetails(details []Models.ReportRoomBookingDetail) error {
	if len(details) == 0 {
		return nil
	}
	return r._db.Save(&details).Error
}
