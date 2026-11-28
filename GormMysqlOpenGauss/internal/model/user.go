package model

import "time"

type User struct {
	ID        uint64 `gorm:"primaryKey;autoIncrement"`
	Name      string `gorm:"size:128;not null"`
	Email     string `gorm:"size:255;not null;uniqueIndex"`
	Age       int    `gorm:"not null;default:0"`
	CreatedAt time.Time
	UpdatedAt time.Time
}

func (User) TableName() string { return "gorm_opengauss_users" }
