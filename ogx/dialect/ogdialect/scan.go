package ogdialect

import (
	"reflect"

	"gitee.com/chentanyang/ogx/schema"
)

func scanner(typ reflect.Type) schema.ScannerFunc {
	return schema.Scanner(typ)
}
