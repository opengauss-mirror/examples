package main

import (
	"context"
	"database/sql"
	"fmt"

	"gitee.com/chentanyang/ogx"
	"gitee.com/chentanyang/ogx/dialect/ogdialect"
	"gitee.com/chentanyang/ogx/extra/ogxdebug"

	_ "gitee.com/opengauss/openGauss-connector-go-pq"
)

type Profile struct {
	ID     int64 `ogx:",pk,autoincrement"`
	Lang   string
	Active bool
	UserID int64
}

// User has many profiles.
type User struct {
	ID       int64 `ogx:",pk,autoincrement"`
	Name     string
	Profiles []*Profile `ogx:"rel:has-many,join:id=user_id"`
}

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

	if err := createSchema(ctx, db, (*User)(nil), (*Profile)(nil)); err != nil {
		panic(err)
	}
	defer func() {
		_ = dropSchema(ctx, db, (*User)(nil), (*Profile)(nil))
	}()

	user := new(User)
	if err := db.NewSelect().
		Model(user).
		Column("user.*").
		Relation("Profiles", func(q *ogx.SelectQuery) *ogx.SelectQuery {
			return q.Where("active IS TRUE")
		}).
		OrderExpr("\"user\".id ASC").
		Limit(1).
		Scan(ctx); err != nil {
		panic(err)
	}
	fmt.Println(user.ID, user.Name, user.Profiles[0], user.Profiles[1])
	// Output: 1 user 1 &{1 en true 1} &{2 ru true 1}
}

func createSchema(ctx context.Context, db *ogx.DB, models ...interface{}) error {
	for _, model := range models {
		if _, err := db.NewCreateTable().Model(model).Exec(ctx); err != nil {
			return err
		}
	}

	users := []*User{
		{ID: 1, Name: "user 1"},
		{ID: 2, Name: "user 2"},
	}
	if _, err := db.NewInsert().Model(&users).Exec(ctx); err != nil {
		return err
	}

	profiles := []*Profile{
		{ID: 1, Lang: "en", Active: true, UserID: 1},
		{ID: 2, Lang: "ru", Active: true, UserID: 1},
		{ID: 3, Lang: "md", Active: false, UserID: 1},
	}
	if _, err := db.NewInsert().Model(&profiles).Exec(ctx); err != nil {
		return err
	}

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
