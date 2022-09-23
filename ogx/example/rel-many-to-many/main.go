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

type Order struct {
	ID int64 `ogx:",pk,autoincrement"`
	// Order and Item in join:Order=Item are fields in OrderToItem model
	Items []Item `ogx:"m2m:order_to_items,join:Order=Item"`
}

type Item struct {
	ID int64 `ogx:",pk,autoincrement"`
	// Order and Item in join:Order=Item are fields in OrderToItem model
	Orders []Order `ogx:"m2m:order_to_items,join:Item=Order"`
}

type OrderToItem struct {
	OrderID int64  `ogx:",pk"`
	Order   *Order `ogx:"rel:belongs-to,join:order_id=id"`
	ItemID  int64  `ogx:",pk"`
	Item    *Item  `ogx:"rel:belongs-to,join:item_id=id"`
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
	db.RegisterModel((*OrderToItem)(nil))

	if err := createSchema(ctx, db, (*Order)(nil), (*Item)(nil), (*OrderToItem)(nil)); err != nil {
		panic(err)
	}
	defer func() {
		_ = dropSchema(ctx, db, (*Item)(nil), (*Order)(nil), (*Item)(nil), (*OrderToItem)(nil))
	}()

	order := new(Order)
	if err := db.NewSelect().
		Model(order).
		Relation("Items").
		Order("order.id ASC").
		Limit(1).
		Scan(ctx); err != nil {
		panic(err)
	}
	fmt.Println("Order", order.ID, "Items", order.Items[0].ID, order.Items[1].ID)
	fmt.Println()

	order = new(Order)
	if err := db.NewSelect().
		Model(order).
		Relation("Items", func(q *ogx.SelectQuery) *ogx.SelectQuery {
			q = q.OrderExpr("item.id DESC")
			return q
		}).
		Limit(1).
		Scan(ctx); err != nil {
		panic(err)
	}
	fmt.Println("Order", order.ID, "Items", order.Items[0].ID, order.Items[1].ID)
	fmt.Println()

	item := new(Item)
	if err := db.NewSelect().
		Model(item).
		Relation("Orders").
		Order("item.id ASC").
		Limit(1).
		Scan(ctx); err != nil {
		panic(err)
	}
	fmt.Println("Item", item.ID, "Order", item.Orders[0].ID)
}

func createSchema(ctx context.Context, db *ogx.DB, models ...interface{}) error {
	for _, model := range models {
		if _, err := db.NewCreateTable().Model(model).Exec(ctx); err != nil {
			return err
		}
	}

	values := []interface{}{
		&Item{ID: 1},
		&Item{ID: 2},
		&Order{ID: 1},
		&OrderToItem{OrderID: 1, ItemID: 1},
		&OrderToItem{OrderID: 1, ItemID: 2},
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
