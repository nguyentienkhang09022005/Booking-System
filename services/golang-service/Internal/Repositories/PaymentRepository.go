package Repositories

import (
	"github.com/google/uuid"
	"gorm.io/gorm"
	"payment-service/Internal/Models"
)

type IPaymentRepository interface {
	Create(payment *Models.Payment) (*Models.Payment, error)
	FindByAppTransID(appTransID string) (*Models.Payment, error)
	UpdateStatus(idPayment uuid.UUID, zpTransId string, status string) error
}
type paymentRepository struct {
	_db *gorm.DB
}

// Construtor
func PaymentRepository(db *gorm.DB) IPaymentRepository {
	return &paymentRepository{_db: db}
}

// Repo tạo thanh toán
func (p *paymentRepository) Create(payment *Models.Payment) (*Models.Payment, error) {
	err := p._db.Create(payment).Error
	if err != nil {
		return nil, err
	}
	return payment, nil
}

func (p *paymentRepository) FindByAppTransID(IDAppTrans string) (*Models.Payment, error) {
	var payment Models.Payment

	err := p._db.
		Where("id_app_trans = ?", IDAppTrans).
		First(&payment).Error

	if err != nil {
		return nil, err
	}

	var booking Models.Booking
	errBooking := p._db.Where("id_booking = ?", payment.IDBooking).First(&booking).Error

	if errBooking == nil {
		payment.Booking = booking
	} else {
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
