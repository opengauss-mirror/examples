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

type Item struct {
	ID       int64  `ogx:",pk,autoincrement"`
	Children []Item `ogx:"m2m:item_to_items,join:Item=Child"`
}

type ItemToItem struct {
	ItemID  int64 `ogx:",pk"`
	Item    *Item `ogx:"rel:belongs-to,join:item_id=id"`
	ChildID int64 `ogx:",pk"`
	Child   *Item `ogx:"rel:belongs-to,join:child_id=id"`
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

	// Register many to many model so ogx can better recognize m2m relation.
	// This should be done before you use the model for the first time.
	db.RegisterModel((*ItemToItem)(nil))

	if err := createSchema(ctx, db, (*Item)(nil), (*ItemToItem)(nil)); err != nil {
		panic(err)
	}
	defer func() {
		_ = dropSchema(ctx, db, (*Item)(nil), (*ItemToItem)(nil))
	}()

	item1 := new(Item)
	if err := db.NewSelect().
		Model(item1).
		Relation("Children").
		Order("item.id ASC").
		Where("id = 1").
		Scan(ctx); err != nil {
		panic(err)
	}

	item2 := new(Item)
	if err := db.NewSelect().
		Model(item2).
		Relation("Children").
		Order("item.id ASC").
		Where("id = 2").
		Scan(ctx); err != nil {
		panic(err)
	}

	fmt.Println("item1", item1.ID, "children", item1.Children[0].ID, item1.Children[1].ID)
	fmt.Println("item2", item2.ID, "children", item2.Children[0].ID, item2.Children[1].ID)
}

func createSchema(ctx context.Context, db *ogx.DB, models ...interface{}) error {
	for _, model := range models {
		if err := db.ResetModel(ctx, model); err != nil {
			return err
		}
	}

	values := []interface{}{
		&Item{ID: 1},
		&Item{ID: 2},
		&Item{ID: 3},
		&Item{ID: 4},
		&ItemToItem{ItemID: 1, ChildID: 2},
		&ItemToItem{ItemID: 1, ChildID: 3},
		&ItemToItem{ItemID: 2, ChildID: 3},
		&ItemToItem{ItemID: 2, ChildID: 4},
	}
	for _, value := range values {
		if _, err := db.NewInsert().Model(value).Exec(ctx); err != nil {
			return err
		}
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
