package db

import (
	"context"
	"database/sql"
	"fmt"
	"strings"
	"time"

	"github.com/opengauss/examples/gorm-mysql-compat/internal/config"
	"gorm.io/driver/mysql"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
	"gorm.io/gorm/migrator"
)

// openGaussDialector keeps the MySQL protocol and SQL behavior, but avoids a
// MySQL-only schema-name normalization query used by the upstream migrator.
type openGaussDialector struct {
	*mysql.Dialector
}

func (d openGaussDialector) Migrator(db *gorm.DB) gorm.Migrator {
	m := d.Dialector.Migrator(db).(mysql.Migrator)
	return openGaussMigrator{Migrator: m}
}

type openGaussMigrator struct {
	mysql.Migrator
}

// CurrentDatabase uses GORM's generic SELECT DATABASE() implementation.
// Dolphin does not provide the MySQL Information_schema.SCHEMATA relation
// expected by mysql.Migrator.CurrentDatabase.
func (m openGaussMigrator) CurrentDatabase() string {
	return m.Migrator.Migrator.CurrentDatabase()
}

// GetTables must use the corrected database name too; the embedded MySQL
// implementation calls its own CurrentDatabase method and would otherwise
// still resolve the database to an empty string.
func (m openGaussMigrator) GetTables() (tableList []string, err error) {
	err = m.DB.Raw("SELECT TABLE_NAME FROM information_schema.tables WHERE TABLE_SCHEMA = ?", m.CurrentDatabase()).
		Scan(&tableList).Error
	return
}

// ColumnTypes uses Dolphin's information_schema.columns, but supplies the
// database name from SELECT DATABASE() instead of the MySQL migrator's
// CurrentSchema method, which depends on the unsupported SCHEMATA relation.
func (m openGaussMigrator) ColumnTypes(value interface{}) ([]gorm.ColumnType, error) {
	columnTypes := make([]gorm.ColumnType, 0)
	err := m.RunWithValue(value, func(stmt *gorm.Statement) error {
		qualifiedTable := stmt.Table
		table := qualifiedTable
		currentDatabase := ""
		if tables := strings.Split(qualifiedTable, "."); len(tables) == 2 {
			currentDatabase, table = tables[0], tables[1]
		} else {
			currentDatabase = m.CurrentDatabase()
		}

		rows, err := m.DB.Session(&gorm.Session{}).Table(qualifiedTable).Limit(1).Rows()
		if err != nil {
			return err
		}
		rawColumnTypes, err := rows.ColumnTypes()
		if err != nil {
			_ = rows.Close()
			return err
		}
		if err := rows.Close(); err != nil {
			return err
		}

		columnTypeSQL := "SELECT column_name, column_default, is_nullable = 'YES', data_type, character_maximum_length, column_type, column_key, extra, column_comment, numeric_precision, numeric_scale"
		if !m.DisableDatetimePrecision {
			columnTypeSQL += ", datetime_precision"
		}
		columnTypeSQL += " FROM information_schema.columns WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position"
		columns, err := m.DB.Session(&gorm.Session{}).Table(qualifiedTable).Raw(columnTypeSQL, currentDatabase, table).Rows()
		if err != nil {
			return err
		}
		defer columns.Close()

		for columns.Next() {
			var (
				column            migrator.ColumnType
				datetimePrecision sql.NullInt64
				columnKey         sql.NullString
				extra             sql.NullString
				values            = []interface{}{
					&column.NameValue, &column.DefaultValueValue, &column.NullableValue,
					&column.DataTypeValue, &column.LengthValue, &column.ColumnTypeValue,
					&columnKey, &extra, &column.CommentValue, &column.DecimalSizeValue, &column.ScaleValue,
				}
			)
			if !m.DisableDatetimePrecision {
				values = append(values, &datetimePrecision)
			}
			if err := columns.Scan(values...); err != nil {
				return err
			}

			column.PrimaryKeyValue = sql.NullBool{Bool: columnKey.String == "PRI", Valid: true}
			column.UniqueValue = sql.NullBool{Bool: columnKey.String == "UNI", Valid: true}
			column.AutoIncrementValue = sql.NullBool{Bool: strings.Contains(extra.String, "auto_increment"), Valid: true}
			if datetimePrecision.Valid {
				column.DecimalSizeValue = datetimePrecision
			}
			if m.DontSupportNullAsDefaultValue && column.DefaultValueValue.Valid && column.DefaultValueValue.String == "NULL" {
				column.DefaultValueValue = sql.NullString{}
			}
			for _, rawColumnType := range rawColumnTypes {
				if rawColumnType.Name() == column.NameValue.String {
					column.SQLColumnType = rawColumnType
					break
				}
			}
			columnTypes = append(columnTypes, column)
		}
		return columns.Err()
	})
	return columnTypes, err
}

// GetIndexes queries Dolphin's lowercase information_schema view directly.
// The upstream MySQL migrator uses INFORMATION_SCHEMA.STATISTICS and its
// CurrentSchema helper, which fails on this protocol endpoint.
func (m openGaussMigrator) GetIndexes(value interface{}) ([]gorm.Index, error) {
	indexes := make([]gorm.Index, 0)
	err := m.RunWithValue(value, func(stmt *gorm.Statement) error {
		qualifiedTable := stmt.Table
		database, table := m.CurrentDatabase(), qualifiedTable
		if tables := strings.Split(qualifiedTable, "."); len(tables) == 2 {
			database, table = tables[0], tables[1]
		}

		rows, err := m.DB.Session(&gorm.Session{}).Table(qualifiedTable).Raw(`
SELECT table_name, column_name, index_name, non_unique
FROM information_schema.statistics
WHERE table_schema = ? AND table_name = ?
ORDER BY index_name, seq_in_index`, database, table).Rows()
		if err != nil {
			return err
		}
		defer rows.Close()

		byName := make(map[string]*migrator.Index)
		var ordered []*migrator.Index
		for rows.Next() {
			var tableName, columnName, indexName string
			var nonUnique int32
			if err := rows.Scan(&tableName, &columnName, &indexName, &nonUnique); err != nil {
				return err
			}
			index, ok := byName[indexName]
			if !ok {
				index = &migrator.Index{
					TableName:       tableName,
					NameValue:       indexName,
					PrimaryKeyValue: sql.NullBool{Bool: indexName == "PRIMARY", Valid: true},
					UniqueValue:     sql.NullBool{Bool: nonUnique == 0, Valid: true},
				}
				byName[indexName] = index
				ordered = append(ordered, index)
			}
			index.ColumnList = append(index.ColumnList, columnName)
		}
		if err := rows.Err(); err != nil {
			return err
		}
		for _, index := range ordered {
			indexes = append(indexes, index)
		}
		return nil
	})
	return indexes, err
}

// SkipInitializeWithVersion avoids MySQL feature probing against openGauss version strings.
func Open(cfg config.Config, logLevel logger.LogLevel) (*gorm.DB, error) {
	dsn, err := cfg.DSNString()
	if err != nil {
		return nil, err
	}
	dialector := openGaussDialector{Dialector: mysql.New(mysql.Config{DSN: dsn, SkipInitializeWithVersion: true, DisableDatetimePrecision: true, DontSupportRenameIndex: true, DontSupportRenameColumn: true, DefaultStringSize: 256}).(*mysql.Dialector)}
	db, err := gorm.Open(dialector, &gorm.Config{
		DisableAutomaticPing: true,
		Logger:               logger.Default.LogMode(logLevel),
	})
	if err != nil {
		return nil, fmt.Errorf("open openGauss B MySQL connection: %w", err)
	}
	return db, nil
}
func HealthCheck(ctx context.Context, db *gorm.DB) error {
	sqlDB, err := db.DB()
	if err != nil {
		return err
	}
	if err := sqlDB.PingContext(ctx); err != nil {
		return fmt.Errorf("ping openGauss: %w", err)
	}
	return nil
}
func Close(db *gorm.DB) error {
	if db == nil {
		return nil
	}
	sqlDB, err := db.DB()
	if err != nil {
		return err
	}
	return sqlDB.Close()
}
func ConfigurePool(db *gorm.DB, maxOpen, maxIdle int, maxLifetime time.Duration) error {
	sqlDB, err := db.DB()
	if err != nil {
		return err
	}
	sqlDB.SetMaxOpenConns(maxOpen)
	sqlDB.SetMaxIdleConns(maxIdle)
	sqlDB.SetConnMaxLifetime(maxLifetime)
	return nil
}
