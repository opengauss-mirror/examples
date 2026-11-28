package config

import (
	"errors"
	"fmt"
	"net/url"
	"os"
	"strconv"
	"strings"
	"time"

	driver "github.com/go-sql-driver/mysql"
)

type Config struct {
	DSN      string
	Host     string
	Port     int
	User     string
	Password string
	Database string
	Params   map[string]string
}

func FromEnv() (Config, error) {
	c := Config{DSN: strings.TrimSpace(os.Getenv("OPENGAUSS_DSN"))}
	if c.DSN != "" {
		if err := validateDSN(c.DSN); err != nil {
			return Config{}, err
		}
		return c, nil
	}
	c.Host = envOr("OPENGAUSS_HOST", "127.0.0.1")
	c.Port = envInt("OPENGAUSS_PORT", 3308)
	c.User = envOr("OPENGAUSS_USER", "gaussdb")
	c.Password = os.Getenv("OPENGAUSS_PASSWORD")
	c.Database = envOr("OPENGAUSS_DATABASE", "postgres")
	if c.Port < 1 || c.Port > 65535 {
		return Config{}, fmt.Errorf("OPENGAUSS_PORT must be between 1 and 65535")
	}
	c.Params = map[string]string{"parseTime": "true", "charset": "utf8mb4", "collation": "utf8mb4_general_ci", "loc": "Local", "multiStatements": "false"}
	return c, nil
}

func (c Config) DSNString() (string, error) {
	if c.DSN != "" {
		if err := validateDSN(c.DSN); err != nil {
			return "", err
		}
		dsn := c.DSN
		for key, value := range map[string]string{
			"allowNativePasswords": "true",
			"charset":              "utf8mb4",
			"collation":            "utf8mb4_general_ci",
			"parseTime":            "true",
			"loc":                  "Local",
			"multiStatements":      "false",
		} {
			dsn = ensureParam(dsn, key, value)
		}
		return dsn, nil
	}
	if c.Host == "" || c.User == "" || c.Database == "" {
		return "", errors.New("host, user and database are required")
	}
	params := c.Params
	if params == nil {
		params = map[string]string{}
	}
	if params["parseTime"] == "" {
		params["parseTime"] = "true"
	}
	if params["charset"] == "" {
		params["charset"] = "utf8mb4"
	}
	if params["collation"] == "" {
		params["collation"] = "utf8mb4_general_ci"
	}
	return (&driver.Config{User: c.User, Passwd: c.Password, Net: "tcp", Addr: fmt.Sprintf("%s:%d", c.Host, c.Port), DBName: c.Database, Loc: time.Local, AllowNativePasswords: true, Params: params}).FormatDSN(), nil
}

func validateDSN(dsn string) error {
	cfg, err := driver.ParseDSN(dsn)
	if err != nil {
		return fmt.Errorf("invalid OPENGAUSS_DSN: %w", err)
	}
	if cfg.Net != "tcp" {
		return errors.New("OPENGAUSS_DSN must use the go-sql-driver/mysql TCP network")
	}
	return nil
}

func ensureParam(dsn, key, value string) string {
	tail := dsn[strings.LastIndexByte(dsn, '/')+1:]
	options, hasQuery := "", false
	if index := strings.IndexByte(tail, '?'); index >= 0 {
		hasQuery = true
		options = tail[index+1:]
	}
	for _, option := range strings.Split(options, "&") {
		name, _, found := strings.Cut(option, "=")
		if found && name == key {
			return dsn
		}
	}
	pair := url.QueryEscape(key) + "=" + url.QueryEscape(value)
	if !hasQuery {
		return dsn + "?" + pair
	}
	if options == "" {
		return dsn + pair
	}
	return dsn + "&" + pair
}
func envOr(name, fallback string) string {
	if v := strings.TrimSpace(os.Getenv(name)); v != "" {
		return v
	}
	return fallback
}
func envInt(name string, fallback int) int {
	v, err := strconv.Atoi(strings.TrimSpace(os.Getenv(name)))
	if err != nil || v == 0 {
		return fallback
	}
	return v
}
