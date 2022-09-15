package main

import (
	"context"
	"database/sql"
	"time"

	"gitee.com/chentanyang/ogx/dialect/ogdialect"

	"github.com/davecgh/go-spew/spew"

	"gitee.com/chentanyang/ogx"
	"gitee.com/chentanyang/ogx/extra/ogxdebug"

	_ "gitee.com/opengauss/openGauss-connector-go-pq"
)

func main() {
	ctx := context.Background()

	connStr := "host=192.168.20.40 port=26000 user=cuih password=Gauss@123 dbname=test sslmode=disable"
	opengaussdb, err := sql.Open("opengauss", connStr)
	if err != nil {
		panic(err)
	}
	opengaussdb.SetMaxOpenConns(1)

	db := ogx.NewDB(opengaussdb, ogdialect.New())
	db.AddQueryHook(ogxdebug.NewQueryHook(ogxdebug.WithVerbose(true)))

	if err := db.ResetModel(ctx, (*User)(nil)); err != nil {
		panic(err)
	}

	defer func() {
		_ = dropSchema(ctx, db, (*User)(nil))
	}()

	user := new(User)

	if _, err := db.NewInsert().Model(user).Exec(ctx); err != nil {
		panic(err)
	}

	if _, err := db.NewUpdate().
		Model(user).
		WherePK().
		Exec(ctx); err != nil {
		panic(err)
	}

	if err := db.NewSelect().Model(user).WherePK().Scan(ctx); err != nil {
		panic(err)
	}

	spew.Dump(user)
}

type User struct {
	ID        int64 `ogx:",pk,autoincrement"`
	Password  string
	CreatedAt time.Time `ogx:",nullzero"`
	UpdatedAt time.Time `ogx:",nullzero"`
}

var _ ogx.BeforeAppendModelHook = (*User)(nil)

func (u *User) BeforeAppendModel(ctx context.Context, query ogx.Query) error {
	switch query.(type) {
	case *ogx.InsertQuery:
		u.Password = "[encrypted]"
		u.CreatedAt = time.Now()
	case *ogx.UpdateQuery:
		u.UpdatedAt = time.Now()
	}
	return nil
}

var _ ogx.BeforeScanRowHook = (*User)(nil)

func (u *User) BeforeScanRow(ctx context.Context) error {
	// Do some initialization.
	u.Password = ""
	return nil
}

var _ ogx.AfterScanRowHook = (*User)(nil)

func (u *User) AfterScanRow(ctx context.Context) error {
	u.Password = "[decrypted]"
	return nil
}

func dropSchema(ctx context.Context, db *ogx.DB, models ...interface{}) error {
	for _, model := range models {
		if _, err := db.NewDropTable().Model(model).IfExists().Cascade().Exec(ctx); err != nil {
			return err
		}
	}
	return nil
}
