# GORM + MySQL Go 驱动连接 openGauss B 兼容模式

本示例验证 GORM 通过 gorm.io/driver/mysql 和 MySQL Go 驱动连接 openGauss 7.0.0-LTS 的 B（dolphin）兼容模式。示例默认端口为 dolphin_server_port 常见值 3308。

## 快速开始

设置 OPENGAUSS_HOST、OPENGAUSS_PORT、OPENGAUSS_USER、OPENGAUSS_PASSWORD、OPENGAUSS_DATABASE 后执行：

`OPENGAUSS_DATABASE` 填写目标 schema 名。Dolphin 通过服务端参数 `dolphin.default_database_name` 选择物理 B 兼容库。例如，服务端指向物理库 `proto_test_db`，示例表要建在该库的 `gorm_test` schema 下，则设置 `OPENGAUSS_DATABASE=gorm_test`。使用 `OPENGAUSS_DSN` 时，连接串中 `/` 后面的名称同样填写 `gorm_test`。运行 migrate 前，请确认该 schema 已创建，连接用户有建表权限。

    go mod tidy
    go run ./cmd/gorm-opengauss -action=health
    go run ./cmd/gorm-opengauss -action=migrate
    go run ./cmd/gorm-opengauss -action=crud

也可以通过环境变量 OPENGAUSS_DSN 提供 go-sql-driver/mysql 格式的连接串。不要将真实口令写入命令历史、源码或日志。

关键适配点包括 SkipInitializeWithVersion: true，以及示例内对 GORM MySQL migrator 的 openGauss 兼容包装。请通过示例的 internal/db.Open 打开连接；不要在此场景直接用原生 mysql.New 替代该包装，否则 AutoMigrate 的数据库、列和索引元数据检查会再次触发 Dolphin 不兼容查询。包装器直接使用 SELECT DATABASE()，并查询 Dolphin 的 information_schema.columns/statistics。parseTime=true 用于正确映射时间列，multiStatements=false 避免拼接 SQL 执行。

dolphin 当前 MySQL 协议文档列出的 MySQL 命令行客户端支持范围为 8.0.28 及以下。使用 MySQL CLI 做独立连通性验证时，建议使用 8.0.28 或以下版本；此限制不等同于 Go 驱动兼容性结论。

## 测试

    go test ./...

不需要数据库即可运行单元测试（sqlmock 用于验证仓储 SQL 构建）。设置 OPENGAUSS_DSN 后执行 health 联调。

## 目录

- internal/config：环境变量和 DSN 解析。
- internal/db：MySQL dialector、连接池和健康检查。
- internal/model、internal/repository：GORM 模型和 CRUD。
- cmd/gorm-opengauss：命令行示例。
- docs：指导文档、测试思维导图和自测报告。

测试报告位于 docs/self_test_report.md，思维导图位于 docs/test_mindmap.md，设计说明位于 doc/设计文档.md；集成测试使用 integration build tag。
