package internal

import (
	"fmt"
	"log"
	"os"
)

var Warn = log.New(os.Stderr, "WARN: ogx: ", log.LstdFlags)

var Deprecated = log.New(os.Stderr, "DEPRECATED: ogx: ", log.LstdFlags)

type Logging interface {
	Printf(format string, v ...interface{})
}

type logger struct {
	log *log.Logger
}

func (l *logger) Printf(format string, v ...interface{}) {
	_ = l.log.Output(2, fmt.Sprintf(format, v...))
}

var Logger Logging = &logger{
	log: log.New(os.Stderr, "ogx: ", log.LstdFlags|log.Lshortfile),
}
