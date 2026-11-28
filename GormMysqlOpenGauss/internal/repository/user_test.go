package repository

import (
	"context"
	"github.com/DATA-DOG/go-sqlmock"
	"github.com/opengauss/examples/gorm-mysql-compat/internal/model"
	"gorm.io/driver/mysql"
	"gorm.io/gorm"
	"testing"
)

func TestUserRepositoryBuildsSQL(t *testing.T) {
	sqlDB, _, err := sqlmock.New()
	if err != nil {
		t.Fatal(err)
	}
	defer sqlDB.Close()
	db, err := gorm.Open(mysql.New(mysql.Config{Conn: sqlDB, SkipInitializeWithVersion: true}), &gorm.Config{DryRun: true, SkipDefaultTransaction: true})
	if err != nil {
		t.Fatal(err)
	}
	r := NewUserRepository(db)
	ctx := context.Background()
	u := &model.User{Name: "A", Email: "a@example.com", Age: 1}
	if err := r.Create(ctx, u); err != nil {
		t.Fatal(err)
	}
	if !IsNotFound(gorm.ErrRecordNotFound) {
		t.Fatal("not-found helper failed")
	}
}
