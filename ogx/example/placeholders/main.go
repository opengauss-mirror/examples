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

type User struct {
	ID     int64 `ogx:",pk,autoincrement"`
	Name   string
	Emails []string
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

	if err := createSchema(ctx, db, (*User)(nil)); err != nil {
		panic(err)
	}
	defer func() {
		_, _ = db.NewDropTable().Model((*User)(nil)).IfExists().Cascade().Exec(ctx)
	}()

	var tableName, tableAlias, pks, tablePKs, columns, tableColumns string

	if err := db.NewSelect().Model((*User)(nil)).
		ColumnExpr("'?TableName'").
		ColumnExpr("'?TableAlias'").
		ColumnExpr("'?PKs'").
		ColumnExpr("'?TablePKs'").
		ColumnExpr("'?Columns'").
		ColumnExpr("'?TableColumns'").
		ModelTableExpr("").
		Scan(ctx, &tableName, &tableAlias, &pks, &tablePKs, &columns, &tableColumns); err != nil {
		panic(err)
	}

	fmt.Println("tableName", tableName)
	fmt.Println("tableAlias", tableAlias)
	fmt.Println("pks", pks)
	fmt.Println("tablePKs", tablePKs)
	fmt.Println("columns", columns)
	fmt.Println("tableColumns", tableColumns)
}

func createSchema(ctx context.Context, db *ogx.DB, models ...interface{}) error {
	for _, model := range models {
		if _, err := db.NewCreateTable().Model(model).Exec(ctx); err != nil {
			return err
		}
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
	return nil
}
