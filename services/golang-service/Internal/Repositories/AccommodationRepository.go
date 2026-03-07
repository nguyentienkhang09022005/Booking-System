package Repositories

import (
	"github.com/google/uuid"
	"gorm.io/gorm"
	"payment-service/Internal/Models"
)

type IAccommodationRepository interface {
	FindById(idAccommodation uuid.UUID) (*Models.Accommodation, error)
	FindTypeRoomsByAccommodation(idHotel uuid.UUID) ([]Models.TypeRoom, error)
}
type accommodationRepository struct {
	_db *gorm.DB
}

// Construtor
func AccommodationRepository(db *gorm.DB) IAccommodationRepository {
	return &accommodationRepository{_db: db}
}

func (h *accommodationRepository) FindById(idAccommodation uuid.UUID) (*Models.Accommodation, error) {
	accommodation := Models.Accommodation{}
	if err := h._db.Where("id_accommodation = ?", idAccommodation).First(&accommodation).Error; err != nil {
		return nil, err
	}
	return &accommodation, nil
}

func (r *accommodationRepository) FindTypeRoomsByAccommodation(idAccommodation uuid.UUID) ([]Models.TypeRoom, error) {
	var typeRooms []Models.TypeRoom

	err := r._db.Table("room").
		Select("DISTINCT type_room").
		Where("id_accommodation = ?", idAccommodation).
		Find(&typeRooms).Error

	return typeRooms, err
}
