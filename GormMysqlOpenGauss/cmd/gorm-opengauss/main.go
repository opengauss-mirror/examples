package main

import (
	"context"
	"flag"
	"fmt"
	"github.com/opengauss/examples/gorm-mysql-compat/internal/config"
	"github.com/opengauss/examples/gorm-mysql-compat/internal/db"
	"github.com/opengauss/examples/gorm-mysql-compat/internal/model"
	"github.com/opengauss/examples/gorm-mysql-compat/internal/repository"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
	"log"
	"time"
)

func main() {
	action := flag.String("action", "crud", "health, migrate or crud")
	flag.Parse()
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	cfg, err := config.FromEnv()
	if err != nil {
		log.Fatal(err)
	}
	gdb, err := db.Open(cfg, logger.Info)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close(gdb)
	if err := db.HealthCheck(ctx, gdb); err != nil {
		log.Fatal(err)
	}
	switch *action {
	case "health":
		fmt.Println("openGauss B MySQL protocol: OK")
	case "migrate":
		runMigrate(gdb)
	case "crud":
		runCRUD(ctx, gdb)
	default:
		log.Fatalf("unknown action %q", *action)
	}
}
func runMigrate(gdb *gorm.DB) {
	if err := gdb.AutoMigrate(&model.User{}); err != nil {
		log.Fatal(err)
	}
	fmt.Println("migration: OK")
}
func runCRUD(ctx context.Context, gdb *gorm.DB) {
	if err := gdb.AutoMigrate(&model.User{}); err != nil {
		log.Fatal(err)
	}
	repo := repository.NewUserRepository(gdb)
	u := &model.User{Name: "Alice", Email: fmt.Sprintf("alice-%d@example.com", time.Now().UnixNano()), Age: 30}
	if err := repo.Create(ctx, u); err != nil {
		log.Fatal(err)
	}
	fmt.Printf("created id=%d\n", u.ID)
	if err := repo.UpdateName(ctx, u.ID, "Alice Updated"); err != nil {
		log.Fatal(err)
	}
	got, err := repo.ByID(ctx, u.ID)
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("read name=%s age=%d\n", got.Name, got.Age)
	if err := repo.Delete(ctx, u.ID); err != nil {
		log.Fatal(err)
	}
	fmt.Println("deleted")
}
