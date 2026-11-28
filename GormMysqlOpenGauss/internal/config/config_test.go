package config

import (
	"strings"
	"testing"
)

func TestDSNStringDefaults(t *testing.T) {
	c := Config{Host: "db", Port: 3306, User: "test_user", Database: "test_db"}
	dsn, err := c.DSNString()
	if err != nil {
		t.Fatal(err)
	}
	for _, part := range []string{"test_user@tcp(db:3306)/test_db?", "charset=utf8mb4", "collation=utf8mb4_general_ci", "parseTime=true", "loc=Local"} {
		if !strings.Contains(dsn, part) {
			t.Fatalf("dsn = %q, missing %q", dsn, part)
		}
	}
}
func TestInvalidDSN(t *testing.T) {
	if _, err := (Config{DSN: "postgres://x"}).DSNString(); err == nil {
		t.Fatal("expected invalid DSN error")
	}
}

func TestDSNValidationUsesMySQLParserAndRequiresTCP(t *testing.T) {
	for _, dsn := range []string{
		"test_user@unix(/tmp/mysql.sock)/test_db",
		"test_user@tcp(db:3308/test_db",
	} {
		if _, err := (Config{DSN: dsn}).DSNString(); err == nil {
			t.Fatalf("DSN %q should be rejected", dsn)
		}
	}
	if _, err := (Config{DSN: "test_user@tcp(db:3308)/test_db"}).DSNString(); err != nil {
		t.Fatalf("valid TCP DSN rejected: %v", err)
	}
}

func TestEnsureParamMatchesCompleteOptionName(t *testing.T) {
	dsn := "test_user@tcp(db:3308)/test_db?parseTime=false&loc=UTC"
	got := ensureParam(dsn, "time", "bad")
	if got != dsn+"&time=bad" {
		t.Fatalf("ensureParam matched a partial option: %q", got)
	}
	if got := ensureParam(dsn, "parseTime", "true"); got != dsn {
		t.Fatalf("ensureParam replaced an existing option: %q", got)
	}
}

func TestRawDSNAddsOpenGaussDefaults(t *testing.T) {
	dsn, err := (Config{DSN: "test_user@tcp(db:3308)/test_db"}).DSNString()
	if err != nil {
		t.Fatal(err)
	}
	for _, part := range []string{
		"allowNativePasswords=true",
		"charset=utf8mb4",
		"collation=utf8mb4_general_ci",
		"parseTime=true",
	} {
		if !strings.Contains(dsn, part) {
			t.Fatalf("dsn = %q, missing %q", dsn, part)
		}
	}
}
