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
	CreateVndPayment(idPayment uuid.UUID, idUser uuid.UUID, idBooking uuid.UUID, amount float64, orderID string) (*Models.Payment, error)
	UpdateVndPaymentSuccess(idPayment uuid.UUID, txHash string) error
	ClaimForProcessing(idPayment uuid.UUID) (bool, error)
	UpdateVndPaymentFailed(idPayment uuid.UUID, txHash string) error
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

func (p *paymentRepository) FindByIDPayment(idPayment uuid.UUID) (*Models.Payment, error) {
	var payment Models.Payment
	err := p._db.Where("id_payment = ?", idPayment).First(&payment).Error
	if err != nil {
		return nil, err
	}

	var booking Models.Booking
	if err := p._db.Where("id_booking = ?", payment.IDBooking).First(&booking).Error; err == nil {
		payment.Booking = booking
	}

	return &payment, nil
}

func (p *paymentRepository) CreateVndPayment(idPayment uuid.UUID, idUser uuid.UUID, idBooking uuid.UUID, amount float64, orderID string) (*Models.Payment, error) {
	payment := &Models.Payment{
		IDPayment:  idPayment,
		IDUser:     idUser,
		IDBooking:  idBooking,
		Amount:     amount,
		Method:     "VNDStablecoin",
		Status:     "Pending",
		IDAppTrans: orderID, // reuse IDAppTrans to store the EIP-712 orderId
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
