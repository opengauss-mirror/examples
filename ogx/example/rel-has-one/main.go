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

// Profile belongs to User.
type Profile struct {
	ID     int64 `ogx:",pk,autoincrement"`
	Lang   string
	UserID int64
}

type User struct {
	ID      int64 `ogx:",pk,autoincrement"`
	Name    string
	Profile *Profile `ogx:"rel:has-one,join:id=user_id"`
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

	var users []User
	if err := db.NewSelect().
		Model(&users).
		Column("user.*").
		Relation("Profile").
		Scan(ctx); err != nil {
		panic(err)
	}

	fmt.Println(len(users), "results")
	fmt.Println(users[0].ID, users[0].Name, users[0].Profile)
	fmt.Println(users[1].ID, users[1].Name, users[1].Profile)
	// Output: 2 results
	// 1 user 1 &{1 en 1}
	// 2 user 2 &{2 ru 2}
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
		{ID: 1, Lang: "en", UserID: 1},
		{ID: 2, Lang: "ru", UserID: 2},
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
