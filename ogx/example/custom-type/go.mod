module gitee.com/chentanyang/ogx/example/custom-type

go 1.18

replace gitee.com/chentanyang/ogx => ../..

replace gitee.com/chentanyang/ogx/dbfixture => ../../dbfixture

replace gitee.com/chentanyang/ogx/extra/ogxdebug => ../../extra/ogxdebug

replace gitee.com/chentanyang/ogx/dialect/ogdialect => ../../dialect/ogdialect

require (
	gitee.com/chentanyang/ogx v1.1.7
	gitee.com/chentanyang/ogx/dialect/ogdialect v0.0.0-20220915042425-85b0183464bc
	gitee.com/chentanyang/ogx/extra/ogxdebug v0.0.0-20220915042425-85b0183464bc
	gitee.com/opengauss/openGauss-connector-go-pq v1.0.4
)

require (
	github.com/fatih/color v1.13.0 // indirect
	github.com/jinzhu/inflection v1.0.0 // indirect
	github.com/mattn/go-colorable v0.1.13 // indirect
	github.com/mattn/go-isatty v0.0.16 // indirect
	github.com/tjfoc/gmsm v1.4.1 // indirect
	github.com/tmthrgd/go-hex v0.0.0-20190904060850-447a3041c3bc // indirect
	github.com/vmihailenco/msgpack/v5 v5.3.5 // indirect
	github.com/vmihailenco/tagparser/v2 v2.0.0 // indirect
	golang.org/x/crypto v0.0.0-20220829220503-c86fa9a7ed90 // indirect
	golang.org/x/sys v0.0.0-20220913175220-63ea55921009 // indirect
	golang.org/x/text v0.3.7 // indirect
	golang.org/x/xerrors v0.0.0-20220907171357-04be3eba64a2 // indirect
	gopkg.in/yaml.v3 v3.0.0-20210107192922-496545a6307b // indirect
)
