package Repositories

import (
	"github.com/google/uuid"
	"gorm.io/gorm"
	"payment-service/Internal/Models"
)

type IUserRepository interface {
	FindById(idUser uuid.UUID) (*Models.User, error)
}
type userRepository struct {
	_db *gorm.DB
}

// Construtor
func UserRepository(db *gorm.DB) IUserRepository {
	return &userRepository{_db: db}
}

func (u *userRepository) FindById(idUser uuid.UUID) (*Models.User, error) {
	user := Models.User{}
	if err := u._db.Where("id_user = ?", idUser).First(&user).Error; err != nil {
		return nil, err
	}
	return &user, nil
}
