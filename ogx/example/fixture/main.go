package main

import (
  "bytes"
  "context"
  "database/sql"
  "fmt"
  "os"
  "time"

  "gitee.com/chentanyang/ogx"
  "gitee.com/chentanyang/ogx/dbfixture"
  "gitee.com/chentanyang/ogx/dialect/ogdialect"
  "gitee.com/chentanyang/ogx/extra/ogxdebug"

  _ "gitee.com/opengauss/openGauss-connector-go-pq"
)

type User struct {
  ID        int64 `ogx:",pk,autoincrement"`
  Name      sql.NullString
  Email     string
  Attrs     map[string]interface{} `ogx:",nullzero"`
  CreatedAt time.Time
  UpdatedAt sql.NullTime
}

type Org struct {
  ID      int64 `ogx:",pk,autoincrement"`
  Name    string
  OwnerID int64
  Owner   *User `ogx:"rel:belongs-to"`
}

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

  // Register models before loading fixtures.
  db.RegisterModel((*User)(nil), (*Org)(nil))
  defer func() {
    _ = dropSchema(ctx, db, (*User)(nil), (*Org)(nil))
  }()
  // Automatically create tables.
  fixture := dbfixture.New(db, dbfixture.WithRecreateTables())

  // Load fixtures.
  if err := fixture.Load(ctx, os.DirFS("."), "fixture.yml"); err != nil {
    panic(err)
  }

  // You can access fixtures by _id and by a primary key.
  fmt.Println("Smith", fixture.MustRow("User.smith").(*User))
  fmt.Println("Org with id=1", fixture.MustRow("Org.pk1").(*Org))

  // Load users and orgs from the database.
  var users []User
  var orgs []Org

  if err := db.NewSelect().Model(&users).OrderExpr("id").Scan(ctx); err != nil {
    panic(err)
  }
  if err := db.NewSelect().Model(&orgs).OrderExpr("id").Scan(ctx); err != nil {
    panic(err)
  }

  // And encode the loaded data back as YAML.

  var buf bytes.Buffer
  enc := dbfixture.NewEncoder(db, &buf)

  if err := enc.Encode(users, orgs); err != nil {
    panic(err)
  }

  fmt.Println("")
  fmt.Println(buf.String())
}

func dropSchema(ctx context.Context, db *ogx.DB, models ...interface{}) error {
  for _, model := range models {
    if _, err := db.NewDropTable().Model(model).IfExists().Cascade().Exec(ctx); err != nil {
      return err
    }
  }
  return nil
}
