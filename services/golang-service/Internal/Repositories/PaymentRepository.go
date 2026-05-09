package Repositories

import (
	"github.com/google/uuid"
	"gorm.io/gorm"
	"payment-service/Internal/Models"
)

type IPaymentRepository interface {
	Create(payment *Models.Payment) (*Models.Payment, error)
	FindByAppTransID(appTransID string) (*Models.Payment, error)
	FindByIDPayment(idPayment uuid.UUID) (*Models.Payment, error)
	UpdateStatus(idPayment uuid.UUID, zpTransId string, status string) error
	CreateVndPayment(idPayment uuid.UUID, idUser uuid.UUID, idBooking *uuid.UUID, idBookingTour *uuid.UUID, idBookingTransport *uuid.UUID, amount float64, orderID string) (*Models.Payment, error)
	UpdateVndPaymentSuccess(idPayment uuid.UUID, txHash string) error
	ClaimForProcessing(idPayment uuid.UUID) (bool, error)
	UpdateVndPaymentFailed(idPayment uuid.UUID, txHash string) error
	GetDB() *gorm.DB
}

type paymentRepository struct {
	_db *gorm.DB
}

// Construtor
func PaymentRepository(db *gorm.DB) IPaymentRepository {
	return &paymentRepository{_db: db}
}

func (p *paymentRepository) Create(payment *Models.Payment) (*Models.Payment, error) {
	err := p._db.Create(payment).Error
	if err != nil {
		return nil, err
	}
	return payment, nil
}

func (p *paymentRepository) FindByAppTransID(IDAppTrans string) (*Models.Payment, error) {
	var payment Models.Payment

	err := p._db.Where("id_app_trans = ?", IDAppTrans).First(&payment).Error
	if err != nil {
		return nil, err
	}

	// Lấy thông tin Booking nếu có
	if payment.IDBooking != nil {
		var booking Models.Booking
		if errBooking := p._db.Where("id_booking = ?", payment.IDBooking).First(&booking).Error; errBooking == nil {
			payment.Booking = &booking
		}
	}

	// Lấy thông tin BookingTour nếu có
	if payment.IDBookingTour != nil {
		var bookingTour Models.BookingTour
		if errTour := p._db.Where("id_booking_tour = ?", payment.IDBookingTour).First(&bookingTour).Error; errTour == nil {
			payment.BookingTour = &bookingTour
		}
	}

	// Lấy thông tin BookingTransport nếu có
	if payment.IDBookingTransport != nil {
		var bookingTransport Models.BookingTransport
		if errTransport := p._db.Where("id_booking_transport = ?", payment.IDBookingTransport).First(&bookingTransport).Error; errTransport == nil {
			payment.BookingTransport = &bookingTransport
		}
	}

	return &payment, nil
}

func (p *paymentRepository) UpdateStatus(idPayment uuid.UUID, zpTransId string, status string) error {
	return p._db.Model(&Models.Payment{}).
		Where("id_payment = ?", idPayment).
		Updates(map[string]interface{}{
			"status":      status,
			"id_zp_trans": zpTransId,
		}).Error
}

func (p *paymentRepository) FindByIDPayment(idPayment uuid.UUID) (*Models.Payment, error) {
	var payment Models.Payment
	err := p._db.Where("id_payment = ?", idPayment).First(&payment).Error
	if err != nil {
		return nil, err
	}

	if payment.IDBooking != nil {
		var booking Models.Booking
		if err := p._db.Where("id_booking = ?", payment.IDBooking).First(&booking).Error; err == nil {
			payment.Booking = &booking
		}
	}

	if payment.IDBookingTour != nil {
		var bookingTour Models.BookingTour
		if err := p._db.Where("id_booking_tour = ?", payment.IDBookingTour).First(&bookingTour).Error; err == nil {
			payment.BookingTour = &bookingTour
		}
	}

	if payment.IDBookingTransport != nil {
		var bookingTransport Models.BookingTransport
		if err := p._db.Where("id_booking_transport = ?", payment.IDBookingTransport).First(&bookingTransport).Error; err == nil {
			payment.BookingTransport = &bookingTransport
		}
	}

	return &payment, nil
}

func (p *paymentRepository) CreateVndPayment(idPayment uuid.UUID, idUser uuid.UUID, idBooking *uuid.UUID, idBookingTour *uuid.UUID, idBookingTransport *uuid.UUID, amount float64, orderID string) (*Models.Payment, error) {
	payment := &Models.Payment{
		IDPayment:          idPayment,
		IDUser:             idUser,
		IDBooking:          idBooking,
		IDBookingTour:      idBookingTour,
		IDBookingTransport: idBookingTransport,
		Amount:             amount,
		Method:             "VNDStablecoin",
		Status:             "Pending",
		IDAppTrans:         orderID,
	}
	if err := p._db.Create(payment).Error; err != nil {
		return nil, err
	}
	return payment, nil
}

func (p *paymentRepository) UpdateVndPaymentSuccess(idPayment uuid.UUID, txHash string) error {
	return p._db.Model(&Models.Payment{}).
		Where("id_payment = ?", idPayment).
		Updates(map[string]interface{}{
			"status":  "Success",
			"tx_hash": txHash,
		}).Error
}

func (p *paymentRepository) ClaimForProcessing(idPayment uuid.UUID) (bool, error) {
	result := p._db.Model(&Models.Payment{}).
		Where("id_payment = ? AND status = ?", idPayment, "Pending").
		Update("status", "Processing")
	if result.Error != nil {
		return false, result.Error
	}
	return result.RowsAffected == 1, nil
}

func (p *paymentRepository) UpdateVndPaymentFailed(idPayment uuid.UUID, txHash string) error {
	updates := map[string]interface{}{"status": "Failed"}
	if txHash != "" {
		updates["tx_hash"] = txHash
	}
	return p._db.Model(&Models.Payment{}).
		Where("id_payment = ?", idPayment).
		Updates(updates).Error
}

func (p *paymentRepository) GetDB() *gorm.DB {
	return p._db
}
