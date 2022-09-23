package main

import (
	"database/sql"
	"fmt"

	"gitee.com/chentanyang/ogx"
	"gitee.com/chentanyang/ogx/dialect/ogdialect"

	_ "gitee.com/opengauss/openGauss-connector-go-pq"
)

type Item struct {
	ID int64 `ogx:",pk,autoincrement"`
}

func main() {
	connStr := "host=192.168.20.40 port=26000 user=cuih password=Gauss@123 dbname=test sslmode=disable"
	opengaussdb, err := sql.Open("opengauss", connStr)
	if err != nil {
		panic(err)
	}
	db := ogx.NewDB(opengaussdb, ogdialect.New())
	defer db.Close()

	q := db.NewSelect().Model((*Item)(nil)).Where("id > ?", 0)

	fmt.Println(q.String())
}
