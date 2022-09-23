package main

import (
	"context"
	"database/sql"
	"os"

	"github.com/davecgh/go-spew/spew"

	"gitee.com/chentanyang/ogx"
	"gitee.com/chentanyang/ogx/dbfixture"
	"gitee.com/chentanyang/ogx/dialect/ogdialect"
	"gitee.com/chentanyang/ogx/extra/ogxdebug"

	_ "gitee.com/opengauss/openGauss-connector-go-pq"
)

type Comment struct {
	TrackableID   int64  // Article.ID or Post.ID
	TrackableType string // "article" or "post"
	Text          string
}

type Article struct {
	ID   int64 `ogx:",pk,autoincrement"`
	Name string

	Comments []Comment `ogx:"rel:has-many,join:id=trackable_id,join:type=trackable_type,polymorphic"`
}

type Post struct {
	ID   int64 `ogx:",pk,autoincrement"`
	Name string

	Comments []Comment `ogx:"rel:has-many,join:id=trackable_id,join:type=trackable_type,polymorphic"`
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

	if err := createSchema(ctx, db); err != nil {
		panic(err)
	}
	defer func() {
		_ = dropSchema(ctx, db, (*Comment)(nil), (*Article)(nil), (*Post)(nil))
	}()

	{
		article := new(Article)
		if err := db.NewSelect().
			Model(article).
			Relation("Comments").
			Where("id = 1").
			Scan(ctx); err != nil {
			panic(err)
		}
		spew.Dump(article)
	}

	{
		post := new(Post)
		if err := db.NewSelect().
			Model(post).
			Relation("Comments").
			Where("id = 1").
			Scan(ctx); err != nil {
			panic(err)
		}
		spew.Dump(post)
	}
}

func createSchema(ctx context.Context, db *ogx.DB) error {
	// Register models for the fixture.
	db.RegisterModel((*Comment)(nil), (*Article)(nil), (*Post)(nil))

	// Create tables and load initial data.
	fixture := dbfixture.New(db, dbfixture.WithRecreateTables())
	if err := fixture.Load(ctx, os.DirFS("."), "fixture.yml"); err != nil {
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
