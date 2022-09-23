package main

import (
	"context"
	"database/sql"

	"gitee.com/chentanyang/ogx"
	"gitee.com/chentanyang/ogx/dialect/ogdialect"
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
	db := ogx.NewDB(opengaussdb, ogdialect.New())
	defer db.Close()

	db.AddQueryHook(ogxdebug.NewQueryHook(ogxdebug.WithVerbose(true)))

	if err := db.ResetModel(ctx, (*User)(nil), (*Profile)(nil)); err != nil {
		panic(err)
	}
	defer func() {
		_ = dropSchema(ctx, db, (*User)(nil), (*Profile)(nil))
	}()

	if err := insertUserAndProfile(ctx, db); err != nil {
		panic(err)
	}

	if err := db.RunInTx(ctx, nil, func(ctx context.Context, tx ogx.Tx) error {
		return insertUserAndProfile(ctx, tx)
	}); err != nil {
		panic(err)
	}
}

func insertUserAndProfile(ctx context.Context, db ogx.IDB) error {
	user := &User{
		Name: "Smith",
	}
	if err := InsertUser(ctx, db, user); err != nil {
		return err
	}

	profile := &Profile{
		UserID: user.ID,
		Email:  "iam@smith.com",
	}
	if err := InsertProfile(ctx, db, profile); err != nil {
		return err
	}

	return nil
}

type User struct {
	ID   int64 `ogx:",pk,autoincrement"`
	Name string
}

func InsertUser(ctx context.Context, db ogx.IDB, user *User) error {
	_, err := db.NewInsert().Model(user).Exec(ctx)
	return err
}

type Profile struct {
	ID     int64 `ogx:",pk,autoincrement"`
	UserID int64
	Email  string
}

func InsertProfile(ctx context.Context, db ogx.IDB, profile *Profile) error {
	_, err := db.NewInsert().Model(profile).Exec(ctx)
	return err
}

func dropSchema(ctx context.Context, db *ogx.DB, models ...interface{}) error {
	for _, model := range models {
		if _, err := db.NewDropTable().Model(model).IfExists().Cascade().Exec(ctx); err != nil {
			return err
		}
	}
	return nil
}
