package main

import (
	"context"
	"database/sql"
	"fmt"
	"log"

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
	var date string
	err = opengaussdb.QueryRow("select current_date").Scan(&date)
	if err != nil {
		log.Fatal(err)
	}
	fmt.Println(date)
	db := ogx.NewDB(opengaussdb, ogdialect.New())
	db.AddQueryHook(ogxdebug.NewQueryHook(
		ogxdebug.WithVerbose(true),
		ogxdebug.FromEnv("ogxDEBUG"),
	))

	if err := resetSchema(ctx, db); err != nil {
		panic(err)
	}
	defer func() {
		_ = dropSchema(ctx, db, (*User)(nil), (*Story)(nil))
	}()

	// Select all users.
	users := make([]User, 0)
	if err := db.NewSelect().Model(&users).OrderExpr("id ASC").Scan(ctx); err != nil {
		panic(err)
	}
	fmt.Printf("all users: %v\n\n", users)

	// Select one user by primary key.
	user1 := new(User)
	if err := db.NewSelect().Model(user1).Where("id = ?", 1).Scan(ctx); err != nil {
		panic(err)
	}
	fmt.Printf("user1: %v\n\n", user1)

	// Select a story and the associated author in a single query.
	story := new(Story)
	if err := db.NewSelect().
		Model(story).
		Relation("Author").
		Limit(1).
		Scan(ctx); err != nil {
		panic(err)
	}
	fmt.Printf("story and the author: %v\n\n", story)

	// Select a user into a map.
	var m map[string]interface{}
	if err := db.NewSelect().
		Model((*User)(nil)).
		Limit(1).
		Scan(ctx, &m); err != nil {
		panic(err)
	}
	fmt.Printf("user map: %v\n\n", m)

	// Select all users scanning each column into a separate slice.
	var ids []int64
	var names []string
	if err := db.NewSelect().
		ColumnExpr("id, name").
		Model((*User)(nil)).
		OrderExpr("id ASC").
		Scan(ctx, &ids, &names); err != nil {
		panic(err)
	}
	fmt.Printf("users columns: %v %v\n\n", ids, names)

}

type User struct {
	ID     int64 `ogx:",pk,autoincrement"`
	Name   string
	Emails []string
}

func (u User) String() string {
	return fmt.Sprintf("User<%d %s %v>", u.ID, u.Name, u.Emails)
}

type Story struct {
	ID       int64 `ogx:",pk,autoincrement"`
	Title    string
	AuthorID int64
	Author   *User `ogx:"rel:belongs-to,join:author_id=id"`
}

func resetSchema(ctx context.Context, db *ogx.DB) error {
	if err := db.ResetModel(ctx, (*User)(nil), (*Story)(nil)); err != nil {
		return err
	}

	users := []User{
		{
			Name:   "admin",
			Emails: []string{"admin1@admin", "admin2@admin"},
		},
		{
			Name:   "root",
			Emails: []string{"root1@root", "root2@root"},
		},
	}
	if _, err := db.NewInsert().Model(&users).Exec(ctx); err != nil {
		return err
	}

	stories := []Story{
		{
			Title:    "Cool story",
			AuthorID: users[0].ID,
		},
	}
	if _, err := db.NewInsert().Model(&stories).Exec(ctx); err != nil {
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
