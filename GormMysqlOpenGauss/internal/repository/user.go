package repository

import (
	"context"
	"errors"
	"github.com/opengauss/examples/gorm-mysql-compat/internal/model"
	"gorm.io/gorm"
)

type UserRepository struct{ db *gorm.DB }

func NewUserRepository(db *gorm.DB) *UserRepository { return &UserRepository{db: db} }
func (r *UserRepository) Create(ctx context.Context, user *model.User) error {
	return r.db.WithContext(ctx).Create(user).Error
}
func (r *UserRepository) ByID(ctx context.Context, id uint64) (*model.User, error) {
	var u model.User
	if err := r.db.WithContext(ctx).First(&u, id).Error; err != nil {
		return nil, err
	}
	return &u, nil
}
func (r *UserRepository) List(ctx context.Context, limit int) ([]model.User, error) {
	if limit <= 0 || limit > 1000 {
		limit = 100
	}
	var us []model.User
	err := r.db.WithContext(ctx).Order("id ASC").Limit(limit).Find(&us).Error
	return us, err
}
func (r *UserRepository) UpdateName(ctx context.Context, id uint64, name string) error {
	q := r.db.WithContext(ctx).Model(&model.User{}).Where("id = ?", id).Update("name", name)
	if q.Error != nil {
		return q.Error
	}
	if q.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}
	return nil
}
func (r *UserRepository) Delete(ctx context.Context, id uint64) error {
	q := r.db.WithContext(ctx).Delete(&model.User{}, id)
	if q.Error != nil {
		return q.Error
	}
	if q.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}
	return nil
}
func IsNotFound(err error) bool { return errors.Is(err, gorm.ErrRecordNotFound) }
