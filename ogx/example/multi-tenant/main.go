package main

import (
	"context"
	"database/sql"
	"os"

	"gitee.com/chentanyang/ogx/dialect/ogdialect"

	"github.com/davecgh/go-spew/spew"

	"gitee.com/chentanyang/ogx"
	"gitee.com/chentanyang/ogx/dbfixture"
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

	// Register models for the fixture.
	db.RegisterModel((*Story)(nil))
	defer func() {
		_ = dropSchema(ctx, db, (*Story)(nil))
	}()

	// Create tables and load initial data.
	fixture := dbfixture.New(db, dbfixture.WithRecreateTables())
	if err := fixture.Load(ctx, os.DirFS("."), "fixture.yml"); err != nil {
		panic(err)
	}

	{
		ctx := context.WithValue(ctx, "tenant_id", 1)
		stories, err := selectStories(ctx, db)
		if err != nil {
			panic(err)
		}
		spew.Dump(stories)
	}
}

func selectStories(ctx context.Context, db *ogx.DB) ([]*Story, error) {
	stories := make([]*Story, 0)
	if err := db.NewSelect().Model(&stories).Scan(ctx); err != nil {
		return nil, err
	}
	return stories, nil
}

type Story struct {
	ID       int64 `ogx:",pk,autoincrement"`
	Title    string
	AuthorID int64
}

var _ ogx.BeforeSelectHook = (*Story)(nil)

func (s *Story) BeforeSelect(ctx context.Context, query *ogx.SelectQuery) error {
	if id := ctx.Value("tenant_id"); id != nil {
		query.Where("author_id = ?", id)
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
