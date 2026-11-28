//go:build integration

package db

import (
	"context"
	"fmt"
	"github.com/opengauss/examples/gorm-mysql-compat/internal/config"
	"github.com/opengauss/examples/gorm-mysql-compat/internal/model"
	"github.com/opengauss/examples/gorm-mysql-compat/internal/repository"
	"gorm.io/gorm/logger"
	"os"
	"testing"
	"time"
)

func TestOpenGaussCRUD(t *testing.T) {
	if os.Getenv("OPENGAUSS_DSN") == "" {
		t.Skip("set OPENGAUSS_DSN to run openGauss integration test")
	}
	cfg, err := config.FromEnv()
	if err != nil {
		t.Fatal(err)
	}
	db, err := Open(cfg, logger.Warn)
	if err != nil {
		t.Fatal(err)
	}
	defer Close(db)
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	if err := HealthCheck(ctx, db); err != nil {
		t.Fatal(err)
	}
	if err := db.AutoMigrate(&model.User{}); err != nil {
		t.Fatal(err)
	}
	r := repository.NewUserRepository(db)
	u := &model.User{Name: "integration", Email: fmt.Sprintf("integration-%d@example.com", time.Now().UnixNano()), Age: 18}
	if err := r.Create(ctx, u); err != nil {
		t.Fatal(err)
	}
	deleted := false
	defer func() {
		if !deleted {
			_ = r.Delete(ctx, u.ID)
		}
	}()
	got, err := r.ByID(ctx, u.ID)
	if err != nil {
		t.Fatal(err)
	}
	if got.Name != u.Name {
		t.Fatalf("name=%q", got.Name)
	}
	if err := r.UpdateName(ctx, u.ID, "integration-updated"); err != nil {
		t.Fatal(err)
	}
	got, err = r.ByID(ctx, u.ID)
	if err != nil {
		t.Fatal(err)
	}
	if got.Name != "integration-updated" {
		t.Fatalf("updated name=%q", got.Name)
	}
	if err := r.Delete(ctx, u.ID); err != nil {
		t.Fatal(err)
	}
	deleted = true
	if _, err := r.ByID(ctx, u.ID); !repository.IsNotFound(err) {
		t.Fatalf("expected deleted user to be not found, got %v", err)
	}
}
