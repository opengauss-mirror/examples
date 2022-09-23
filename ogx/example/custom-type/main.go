package main

import (
	"context"
	"database/sql"
	"database/sql/driver"
	"fmt"
	"time"

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

	src := Now()
	var dest Time
	if err := db.NewSelect().ColumnExpr("?", src).Scan(ctx, &dest); err != nil {
		panic(err)
	}

	fmt.Println("src", src)
	fmt.Println("dest", dest)
}

const timeFormat = "15:04:05.999999999"

type Time struct {
	time.Time
}

func NewTime(t time.Time) Time {
	t = t.UTC()
	return Time{
		Time: time.Date(0, 1, 1, t.Hour(), t.Minute(), t.Second(), t.Nanosecond(), time.UTC),
	}
}

func Now() Time {
	return NewTime(time.Now())
}

var _ driver.Valuer = (*Time)(nil)

// Value returns the time as string using timeFormat.
func (tm Time) Value() (driver.Value, error) {
	return tm.UTC().Format(timeFormat), nil
}

var _ sql.Scanner = (*Time)(nil)

// Scan scans the time parsing it if necessary using timeFormat.
func (tm *Time) Scan(src interface{}) (err error) {
	switch src := src.(type) {
	case time.Time:
		*tm = NewTime(src)
		return nil
	case string:
		tm.Time, err = time.ParseInLocation(timeFormat, src, time.UTC)
		return err
	case []byte:
		tm.Time, err = time.ParseInLocation(timeFormat, string(src), time.UTC)
		return err
	case nil:
		tm.Time = time.Time{}
		return nil
	default:
		return fmt.Errorf("unsupported data type: %T", src)
	}
}
