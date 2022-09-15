package main

import (
	"database/sql"
	"fmt"
	"strings"

	"gitee.com/chentanyang/ogx/dialect/ogdialect"

	"gitee.com/chentanyang/ogx"

	_ "gitee.com/opengauss/openGauss-connector-go-pq"
)

type Item struct {
	ID int64 `ogx:",pk,autoincrement"`
}

func main() {
	connStr := "host=192.168.20.40 port=26000 user=cuih password=Gauss@123 dbname=test sslmode=disable"
	opengaussdb, err := sql.Open("opengauss", connStr)
	//sqldb, err := sql.Open(sqliteshim.ShimName, "file::memory:?cache=shared")
	if err != nil {
		panic(err)
	}

	db := ogx.NewDB(opengaussdb, ogdialect.New())
	defer db.Close()

	q := db.NewSelect().Model((*Item)(nil)).Where("id > ?", 0).Where("id < ?", 10)

	fmt.Println(GetWhereFields(q.String()))
}

func GetWhereFields(query string) []string {
	q := strings.Split(query, "WHERE ")
	if len(q) == 1 {
		return nil
	}

	whereFields := strings.Split(q[1], " AND ")

	fields := make([]string, len(whereFields))

	for i := range whereFields {
		fields[i] = strings.TrimFunc(whereFields[i], func(r rune) bool {
			return r == '(' || r == ')'
		})
	}

	return fields
}
