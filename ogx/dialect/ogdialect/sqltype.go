package ogdialect

import (
	"encoding/json"
	"net"
	"reflect"

	"gitee.com/chentanyang/ogx/dialect/sqltype"
	"gitee.com/chentanyang/ogx/schema"
)

const (
	// Date / Time
	ogTypeTimestampTz = "TIMESTAMPTZ"         // Timestamp with a time zone
	ogTypeDate        = "DATE"                // Date
	ogTypeTime        = "TIME"                // Time without a time zone
	ogTypeTimeTz      = "TIME WITH TIME ZONE" // Time with a time zone
	ogTypeInterval    = "INTERVAL"            // Time Interval

	// Network Addresses
	ogTypeInet    = "INET"    // IPv4 or IPv6 hosts and networks
	ogTypeCidr    = "CIDR"    // IPv4 or IPv6 networks
	ogTypeMacaddr = "MACADDR" // MAC addresses

	// Serial Types
	ogTypeSmallSerial = "SMALLSERIAL" // 2 byte autoincrementing integer
	ogTypeSerial      = "SERIAL"      // 4 byte autoincrementing integer
	ogTypeBigSerial   = "BIGSERIAL"   // 8 byte autoincrementing integer

	// Character Types
	ogTypeChar = "CHAR" // fixed length string (blank padded)
	ogTypeText = "TEXT" // variable length string without limit

	// JSON Types
	ogTypeJSON  = "JSON"  // text representation of json data
	ogTypeJSONB = "JSONB" // binary representation of json data

	// Binary Data Types
	ogTypeBytea = "BYTEA" // binary string
)

var (
	ipType             = reflect.TypeOf((*net.IP)(nil)).Elem()
	ipNetType          = reflect.TypeOf((*net.IPNet)(nil)).Elem()
	jsonRawMessageType = reflect.TypeOf((*json.RawMessage)(nil)).Elem()
)

func fieldSQLType(field *schema.Field) string {
	if field.UserSQLType != "" {
		return field.UserSQLType
	}

	if v, ok := field.Tag.Option("composite"); ok {
		return v
	}
	if field.Tag.HasOption("hstore") {
		return sqltype.HSTORE
	}

	if field.Tag.HasOption("array") {
		switch field.IndirectType.Kind() {
		case reflect.Slice, reflect.Array:
			sqlType := sqlType(field.IndirectType.Elem())
			return sqlType + "[]"
		}
	}

	if field.DiscoveredSQLType == sqltype.Blob {
		return ogTypeBytea
	}

	return sqlType(field.IndirectType)
}

func sqlType(typ reflect.Type) string {
	switch typ {
	case ipType:
		return ogTypeInet
	case ipNetType:
		return ogTypeCidr
	case jsonRawMessageType:
		return ogTypeJSONB
	}

	sqlType := schema.DiscoverSQLType(typ)
	switch sqlType {
	case sqltype.Timestamp:
		sqlType = ogTypeTimestampTz
	}

	switch typ.Kind() {
	case reflect.Map, reflect.Struct:
		if sqlType == sqltype.VarChar {
			return ogTypeJSONB
		}
		return sqlType
	case reflect.Array, reflect.Slice:
		if typ.Elem().Kind() == reflect.Uint8 {
			return ogTypeBytea
		}
		return ogTypeJSONB
	}

	return sqlType
}
