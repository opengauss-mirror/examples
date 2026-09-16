# PeeweeTest：peewee × openGauss B 兼容库（dolphin MySQL 协议）适配测试

本目录用于测试验证 python ORM 框架 **peewee** 配合 **PyMySQL** 驱动连接 openGauss **B 兼容模式**数据库（dolphin 插件 MySQL 协议兼容）的能力，并沉淀可重复运行的自动化测试与自测报告。

## 任务与修复追踪

- 统一进度跟踪：<https://gitcode.com/opengauss/opensource-intership/issues/35>
- 历史缺陷记录（已关闭）：<https://gitcode.com/opengauss/Plugin/issues/1626>
- Plugin 修复：<https://gitcode.com/opengauss/Plugin/pull/2553>
- openGauss-server 修复：<https://gitcode.com/opengauss/openGauss-server/pull/9709>
- Peewee 开发指导：<https://gitcode.com/opengauss/docs/pull/8846>

最终联合回归已严格验证：

```text
SHOW INDEX.Key_name = PRIMARY
information_schema.columns.extra = auto_increment
data_type = decimal
column_type = decimal(10,2)
Peewee primary_key = True
Peewee identity = True
MySQL duplicate key = 1062 / 23000
PyMySQL exception = IntegrityError
Peewee exception = IntegrityError
```

Plugin #2553 和 openGauss-server #9709 均已合入。2026-09-16 使用同一组 Server/Plugin master 构建完成两轮全量回归，两轮均为 `48 passed, 0 skipped`；原始 RC3 环境的 `46 passed, 2 failed` 仅作为修复前基线保留在自测报告中。

## 技术链路

```text
Peewee ORM
    → PyMySQL 驱动
    → Dolphin MySQL 协议端口 3308
    → openGauss B 兼容物理数据库
    → MySQL database 对应的业务 schema
```

## 环境要求

- openEuler 20.03 LTS，x86_64 或 aarch64；
- 任务目标版本为 openGauss 7.0.0-LTS；最终联合实测版本为 openGauss 7.0.0 build `9823273a`，版本和原始 RC3 基线差异见自测报告；
- 已创建 B 兼容数据库，并确认 Dolphin 扩展已加载；
- B 兼容物理数据库必须显式使用 `ENCODING='UTF8'`，避免字符字段被映射为二进制类型；
- 已配置 `enable_dolphin_proto=on` 和实际的 `dolphin_server_port`；
- 已设置 MySQL native 密码，并在 `pg_hba.conf` 中放行客户端地址；
- 已在 B 兼容数据库内创建连接参数 `database` 对应的业务 schema，并将其授权给连接用户；
- 使用 PyMySQL 默认反斜杠转义时，连接用户的 `standard_conforming_strings` 应为 `off`；可由管理员执行 `ALTER USER <user> SET standard_conforming_strings TO off;`，并重新建立连接；
- Python 3.10+，安装本项目实测的固定版本依赖：

  ```bash
  pip install -r requirements.txt
  ```

注意：openGauss 的物理数据库与 MySQL 的 `database` 概念不同。在 Dolphin 协议连接中，`dolphin.default_database_name` 指定 openGauss B 兼容物理数据库，而 Peewee/PyMySQL 的 `database` 参数对应该 B 库内的 schema。

## 快速开始

1. 复制 `config.example.ini` 为 `config.ini` 并填写连接信息（或设置 `PEEWEE_TEST_*` 环境变量）。
2. 运行全部测试：
   ```bash
   pytest tests/ -v
   ```
3. 运行单个文件：
   ```bash
   pytest tests/test_crud.py -v
   ```

## 目录结构

```
PeeweeTest/
├── README.md               # 本文件
├── requirements.txt        # 依赖：peewee / pymysql / pytest
├── config.example.ini      # 连接配置模板（config.ini 不入库）
├── docs/
│   ├── 测试思维导图.md      # Mermaid 可维护源文件
│   ├── 测试思维导图.png     # 测试范围思维导图
│   ├── 用例清单.md          # TC 编号与 pytest 函数映射
│   └── 自测报告.md          # 原始基线、缺陷证据与最终验收条件
├── scripts/
│   └── reproduce_dolphin_protocol_issues.py # 元数据与重复键问题最小复现
└── tests/
    ├── conftest.py         # 连接管理、用例后自动清理 test_ 前缀表
    ├── test_connection.py  # 连接、版本、元数据
    ├── test_ddl.py         # 建表/删表/字段类型映射
    ├── test_crud.py        # 插入(含批量)/查询/更新/删除/自增回读
    ├── test_query.py       # 条件/排序/分页/聚合/模糊
    ├── test_transaction.py # 事务/回滚/保存点/autocommit
    ├── test_relations.py   # 外键/一对多/多对多
    ├── test_datatypes.py   # 数据类型与边界
    ├── test_exceptions.py  # 异常与错误处理
    └── test_charset.py     # 字符集（utf8mb4/emoji/特殊字符）
```

## 约定

- 测试建的表统一使用 `test_` 前缀，`conftest.py` 会在每个用例结束后自动删除，保证可重复执行。
- 连接信息**禁止硬编码**：一律走环境变量或 config.ini。
- 遇到失败用例，按"四层对照法"定位：peewee 日志 → PyMySQL 手工执行 → mysql 客户端 → gsql，结论记入自测报告。
