package db

import (
	"regexp"
	"testing"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/opengauss/examples/gorm-mysql-compat/internal/model"
	"gorm.io/driver/mysql"
	"gorm.io/gorm"
)

func TestOpenGaussMigratorUsesCurrentDatabaseForTableChecks(t *testing.T) {
	sqlDB, mock, err := sqlmock.New()
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = sqlDB.Close() })

	dialector := openGaussDialector{Dialector: mysql.New(mysql.Config{
		Conn:                      sqlDB,
		SkipInitializeWithVersion: true,
		DisableDatetimePrecision:  true,
	}).(*mysql.Dialector)}
	db, err := gorm.Open(dialector, &gorm.Config{DisableAutomaticPing: true})
	if err != nil {
		t.Fatal(err)
	}

	mock.ExpectQuery(regexp.QuoteMeta("SELECT DATABASE()")).
		WillReturnRows(sqlmock.NewRows([]string{"DATABASE()"}).AddRow("gorm_test"))
	if got := db.Migrator().CurrentDatabase(); got != "gorm_test" {
		t.Fatalf("CurrentDatabase() = %q, want gorm_test", got)
	}

	mock.ExpectQuery(regexp.QuoteMeta("SELECT DATABASE()")).
		WillReturnRows(sqlmock.NewRows([]string{"DATABASE()"}).AddRow("gorm_test"))
	mock.ExpectQuery(regexp.QuoteMeta("SELECT count(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ? AND table_type = ?")).
		WithArgs("gorm_test", "gorm_opengauss_users", "BASE TABLE").
		WillReturnRows(sqlmock.NewRows([]string{"count(*)"}).AddRow(1))
	if !db.Migrator().HasTable("gorm_opengauss_users") {
		t.Fatal("HasTable() = false, want true")
	}

	if err := mock.ExpectationsWereMet(); err != nil {
		t.Fatal(err)
	}
}

func TestOpenGaussMigratorGetTablesUsesCurrentDatabase(t *testing.T) {
	sqlDB, mock, err := sqlmock.New()
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = sqlDB.Close() })

	dialector := openGaussDialector{Dialector: mysql.New(mysql.Config{
		Conn:                      sqlDB,
		SkipInitializeWithVersion: true,
	}).(*mysql.Dialector)}
	db, err := gorm.Open(dialector, &gorm.Config{DisableAutomaticPing: true})
	if err != nil {
		t.Fatal(err)
	}

	mock.ExpectQuery(regexp.QuoteMeta("SELECT DATABASE()")).
		WillReturnRows(sqlmock.NewRows([]string{"DATABASE()"}).AddRow("gorm_test"))
	mock.ExpectQuery(regexp.QuoteMeta("SELECT TABLE_NAME FROM information_schema.tables WHERE TABLE_SCHEMA = ?")).
		WithArgs("gorm_test").
		WillReturnRows(sqlmock.NewRows([]string{"TABLE_NAME"}).AddRow("gorm_opengauss_users"))

	tables, err := db.Migrator().GetTables()
	if err != nil {
		t.Fatal(err)
	}
	if len(tables) != 1 || tables[0] != "gorm_opengauss_users" {
		t.Fatalf("GetTables() = %#v, want [gorm_opengauss_users]", tables)
	}

	if err := mock.ExpectationsWereMet(); err != nil {
		t.Fatal(err)
	}
}

func TestOpenGaussMigratorColumnTypesAvoidsMySQLSchemaLookup(t *testing.T) {
	sqlDB, mock, err := sqlmock.New()
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = sqlDB.Close() })

	dialector := openGaussDialector{Dialector: mysql.New(mysql.Config{
		Conn:                      sqlDB,
		SkipInitializeWithVersion: true,
		DisableDatetimePrecision:  true,
	}).(*mysql.Dialector)}
	db, err := gorm.Open(dialector, &gorm.Config{DisableAutomaticPing: true})
	if err != nil {
		t.Fatal(err)
	}

	mock.ExpectQuery(regexp.QuoteMeta("SELECT DATABASE()")).
		WillReturnRows(sqlmock.NewRows([]string{"DATABASE()"}).AddRow("gorm_test"))
	mock.ExpectQuery(regexp.QuoteMeta("SELECT * FROM `gorm_opengauss_users` LIMIT ?")).
		WithArgs(1).
		WillReturnRows(sqlmock.NewRows([]string{"id", "name", "email"}).AddRow(1, "Alice", "alice@example.com"))
	mock.ExpectQuery(regexp.QuoteMeta("SELECT column_name, column_default, is_nullable = 'YES', data_type, character_maximum_length, column_type, column_key, extra, column_comment, numeric_precision, numeric_scale FROM information_schema.columns WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position")).
		WithArgs("gorm_test", "gorm_opengauss_users").
		WillReturnRows(sqlmock.NewRows([]string{"column_name", "column_default", "is_nullable", "data_type", "character_maximum_length", "column_type", "column_key", "extra", "column_comment", "numeric_precision", "numeric_scale"}).
			AddRow("id", nil, false, "bigint", nil, "bigint unsigned", "PRI", "auto_increment", "", 20, 0).
			AddRow("name", nil, false, "varchar", 128, "varchar(128)", "", "", "", nil, nil).
			AddRow("email", nil, false, "varchar", 255, "varchar(255)", "UNI", "", "", nil, nil))

	columns, err := db.Migrator().ColumnTypes(&model.User{})
	if err != nil {
		t.Fatal(err)
	}
	if len(columns) != 3 || columns[0].Name() != "id" || columns[1].Name() != "name" || columns[2].Name() != "email" {
		t.Fatalf("ColumnTypes() returned unexpected columns: %#v", columns)
	}
	if got, ok := columns[1].ColumnType(); !ok || got != "varchar(128)" {
		t.Fatalf("name column type = (%q, %v), want (varchar(128), true)", got, ok)
	}

	if err := mock.ExpectationsWereMet(); err != nil {
		t.Fatal(err)
	}
}

func TestOpenGaussMigratorGetIndexesUsesDolphinMetadata(t *testing.T) {
	sqlDB, mock, err := sqlmock.New()
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = sqlDB.Close() })

	dialector := openGaussDialector{Dialector: mysql.New(mysql.Config{
		Conn:                      sqlDB,
		SkipInitializeWithVersion: true,
	}).(*mysql.Dialector)}
	db, err := gorm.Open(dialector, &gorm.Config{DisableAutomaticPing: true})
	if err != nil {
		t.Fatal(err)
	}

	mock.ExpectQuery(regexp.QuoteMeta("SELECT DATABASE()")).
		WillReturnRows(sqlmock.NewRows([]string{"DATABASE()"}).AddRow("gorm_test"))
	mock.ExpectQuery("(?s)SELECT table_name, column_name, index_name, non_unique\\s+FROM information_schema\\.statistics\\s+WHERE table_schema = \\? AND table_name = \\?\\s+ORDER BY index_name, seq_in_index").
		WithArgs("gorm_test", "gorm_opengauss_users").
		WillReturnRows(sqlmock.NewRows([]string{"table_name", "column_name", "index_name", "non_unique"}).
			AddRow("gorm_opengauss_users", "id", "PRIMARY", 0).
			AddRow("gorm_opengauss_users", "email", "idx_gorm_opengauss_users_email", 0))

	indexes, err := db.Migrator().GetIndexes(&model.User{})
	if err != nil {
		t.Fatal(err)
	}
	if len(indexes) != 2 || indexes[0].Name() != "PRIMARY" || indexes[1].Name() != "idx_gorm_opengauss_users_email" {
		t.Fatalf("GetIndexes() returned unexpected indexes: %#v", indexes)
	}
	if got := indexes[1].Columns(); len(got) != 1 || got[0] != "email" {
		t.Fatalf("email index columns = %#v, want [email]", got)
	}

	if err := mock.ExpectationsWereMet(); err != nil {
		t.Fatal(err)
	}
}
