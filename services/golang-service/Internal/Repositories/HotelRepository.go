package Repositories

import (
	"github.com/google/uuid"
	"gorm.io/gorm"
	"payment-service/Internal/Models"
)

type IHotelRepository interface {
	FindById(idHotel uuid.UUID) (*Models.Hotel, error)
}
type hotelRepository struct {
	_db *gorm.DB
}

// Construtor
func HotelRepository(db *gorm.DB) IHotelRepository {
	return &hotelRepository{_db: db}
}

func (h *hotelRepository) FindById(idHotel uuid.UUID) (*Models.Hotel, error) {
	hotel := Models.Hotel{}
	if err := h._db.Where("id_hotel = ?", idHotel).First(&hotel).Error; err != nil {
		return nil, err
	}
	return &hotel, nil
}
