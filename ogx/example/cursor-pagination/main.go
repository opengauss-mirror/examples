package main

import (
	"context"
	"database/sql"
	"fmt"
	"sort"

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
	opengaussdb.SetMaxOpenConns(1)

	db := ogx.NewDB(opengaussdb, ogdialect.New())
	db.AddQueryHook(ogxdebug.NewQueryHook(
		ogxdebug.WithVerbose(true),
		ogxdebug.FromEnv("ogxDEBUG"),
	))

	if err := resetDB(ctx, db); err != nil {
		panic(err)
	}
	defer func() {
		_ = dropSchema(ctx, db, (*Entry)(nil))
	}()

	page1, cursor, err := selectNextPage(ctx, db, 0)
	if err != nil {
		panic(err)
	}

	page2, cursor, err := selectNextPage(ctx, db, cursor.End)
	if err != nil {
		panic(err)
	}

	page3, cursor, err := selectNextPage(ctx, db, cursor.End)
	if err != nil {
		panic(err)
	}

	prevPage, _, err := selectPrevPage(ctx, db, cursor.Start)
	if err != nil {
		panic(err)
	}

	fmt.Println("page #1", page1)
	fmt.Println("page #2", page2)
	fmt.Println("page #3", page3)
	fmt.Println("prev page", prevPage)
}

type Entry struct {
	ID   int64 `ogx:",pk,autoincrement"`
	Text string
}

func (e Entry) String() string {
	return fmt.Sprint(e.ID)
}

// Cursor holds pointers to the first and last items on a page.
// It is used with cursor-based pagination.
type Cursor struct {
	Start int64 // pointer to the first item for the previous page
	End   int64 // pointer to the last item for the next page
}

func NewCursor(entries []Entry) Cursor {
	if len(entries) == 0 {
		return Cursor{}
	}
	return Cursor{
		Start: entries[0].ID,
		End:   entries[len(entries)-1].ID,
	}
}

func selectNextPage(ctx context.Context, db *ogx.DB, cursor int64) ([]Entry, Cursor, error) {
	var entries []Entry
	if err := db.NewSelect().
		Model(&entries).
		Where("id > ?", cursor).
		OrderExpr("id ASC").
		Limit(10).
		Scan(ctx); err != nil {
		return nil, Cursor{}, err
	}
	return entries, NewCursor(entries), nil
}

func selectPrevPage(ctx context.Context, db *ogx.DB, cursor int64) ([]Entry, Cursor, error) {
	var entries []Entry
	if err := db.NewSelect().
		Model(&entries).
		Where("id < ?", cursor).
		OrderExpr("id DESC").
		Limit(10).
		Scan(ctx); err != nil {
		return nil, Cursor{}, err
	}
	sort.Slice(entries, func(i, j int) bool {
		return entries[i].ID < entries[j].ID
	})
	return entries, NewCursor(entries), nil
}

func resetDB(ctx context.Context, db *ogx.DB) error {
	if err := db.ResetModel(ctx, (*Entry)(nil)); err != nil {
		return err
	}

	seed := make([]Entry, 100)

	for i := range seed {
		seed[i] = Entry{ID: int64(i + 1)}
	}

	if _, err := db.NewInsert().Model(&seed).Exec(ctx); err != nil {
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
