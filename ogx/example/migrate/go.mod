module gitee.com/chentanyang/ogx/example/migrate

go 1.18

replace gitee.com/chentanyang/ogx => ../..

replace gitee.com/chentanyang/ogx/dbfixture => ../../dbfixture

replace gitee.com/chentanyang/ogx/extra/ogxdebug => ../../extra/ogxdebug

replace gitee.com/chentanyang/ogx/dialect/ogdialect => ../../dialect/ogdialect

require (
	gitee.com/chentanyang/ogx v1.1.7
	gitee.com/chentanyang/ogx/dialect/ogdialect v0.0.0-20220915060802-59ab6779ee15
	gitee.com/chentanyang/ogx/extra/ogxdebug v0.0.0-20220915060802-59ab6779ee15
	gitee.com/opengauss/openGauss-connector-go-pq v1.0.4
	github.com/urfave/cli/v2 v2.17.1
)

require (
	github.com/cpuguy83/go-md2man/v2 v2.0.2 // indirect
	github.com/fatih/color v1.13.0 // indirect
	github.com/jinzhu/inflection v1.0.0 // indirect
	github.com/mattn/go-colorable v0.1.13 // indirect
	github.com/mattn/go-isatty v0.0.16 // indirect
	github.com/russross/blackfriday/v2 v2.1.0 // indirect
	github.com/tjfoc/gmsm v1.4.1 // indirect
	github.com/tmthrgd/go-hex v0.0.0-20190904060850-447a3041c3bc // indirect
	github.com/vmihailenco/msgpack/v5 v5.3.5 // indirect
	github.com/vmihailenco/tagparser/v2 v2.0.0 // indirect
	github.com/xrash/smetrics v0.0.0-20201216005158-039620a65673 // indirect
	golang.org/x/crypto v0.0.0-20220926161630-eccd6366d1be // indirect
	golang.org/x/sys v0.0.0-20220928140112-f11e5e49a4ec // indirect
	golang.org/x/text v0.3.7 // indirect
	golang.org/x/xerrors v0.0.0-20220907171357-04be3eba64a2 // indirect
)
