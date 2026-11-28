# openGauss B MySQL 协议真实联调记录

## 证据来源与脱敏说明

本记录根据用户在 Windows PowerShell 中粘贴的实际 `go run` 输出整理，时间为 2026-10-08。原始输出中的本机用户名和绝对路径不在此重复；未记录密码、完整 DSN 或客户端 IP。

这份材料是基于聊天中提供的命令输出摘录，并非 PowerShell 原始 transcript 文件。当前可见的 PowerShell 输出没有显示 Go 版本、openEuler 小版本、虚拟机 CPU 架构或目标 IP，因此本记录不对这些信息作推断。此前 gsql 输出所示的 openGauss build 不属于本附件所依据的 PowerShell 记录，未在此列入。

## 当前可确认的环境信息

| 项目 | 记录 |
|---|---|
| 客户端终端 | Windows PowerShell（由命令路径及 `PS C:\...>` 提示符确认） |
| Go 版本 / GOOS / GOARCH | 当前 PowerShell 输出未显示，未记录 |
| 数据库 | `gorm_test`（由 GORM SQL 日志中的 `SELECT DATABASE()` 后续元数据查询确认） |
| 服务端 openGauss build | 当前 PowerShell 输出未显示，未记录 |
| 服务端操作系统版本 | 当前 PowerShell 输出未显示，未记录 |
| 虚拟机 CPU 架构 | 当前 PowerShell 输出未显示，未记录 |
| MySQL 协议目标地址 | 当前 PowerShell 输出未显示，未记录 |

## 执行命令

在 `examples/GormMysqlOpenGauss` 模块目录执行：

```powershell
go run ./cmd/gorm-opengauss -action=migrate
go run ./cmd/gorm-opengauss -action=crud
```

## 结果

### 重复迁移

用户在已有 `gorm_test.gorm_opengauss_users` 表的数据库上重新执行迁移。输出显示表存在性、列信息和索引信息均通过 Dolphin 兼容元数据查询读取，最终输出：

```text
SELECT count(*) FROM information_schema.tables WHERE table_schema = 'gorm_test' AND table_name = 'gorm_opengauss_users' AND table_type = 'BASE TABLE'
SELECT column_name, column_default, is_nullable = 'YES', data_type, character_maximum_length, column_type, column_key, extra, column_comment, numeric_precision, numeric_scale FROM information_schema.columns WHERE table_schema = 'gorm_test' AND table_name = 'gorm_opengauss_users' ORDER BY ordinal_position
SELECT table_name, column_name, index_name, non_unique FROM information_schema.statistics WHERE table_schema = 'gorm_test' AND table_name = 'gorm_opengauss_users' ORDER BY index_name, seq_in_index
migration: OK
```

该次输出未见 `Information_schema.SCHEMATA`、`INFORMATION_SCHEMA.STATISTICS` 错误或 `ALTER TABLE ... MODIFY COLUMN`。

### CRUD

同一示例的 `crud` 命令完成自动迁移及以下操作：

```text
INSERT ...
created id=2
UPDATE ... WHERE id = 2
read name=Alice Updated age=30
DELETE ... WHERE ... id = 2   [rows:1]
deleted
```

结果：创建成功，更新值可读回，删除影响 1 行。自增 ID 为 2 是当时数据库返回值，不要求每次测试均相同。

## 尚未由本记录证明的项目

- 带 `integration` build tag 的 Go 集成测试是否在该真实数据库上运行成功。
- 虚拟机架构是否为 x86_64 或 aarch64。
- 服务端 openGauss build、openEuler 版本，以及当前客户端 Go 版本。

因此本记录仅证明上述两条 `go run` 命令在用户提供的 PowerShell 输出中成功，不替代环境信息采集或 `go test -tags=integration` 的测试结果。
