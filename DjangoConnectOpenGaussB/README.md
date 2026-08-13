# DjangoConnectOpenGaussB

openGauss B 兼容模式 + MySQL 协议（dolphin）下，使用 **原生** `django.db.backends.mysql` + PyMySQL 连接与 CRUD 示例。

## 环境

- openEuler 20.03 LTS
- openGauss 7.0.x 企业版/Server（需 dolphin，Lite 不支持）
- 需合入 Plugin 补丁：支持 `@@default_storage_engine` / `@@sql_auto_is_null`（兼容桩，固定返回值）
- Python 3.8+
- Django 4.2+/5.0
- 驱动：PyMySQL（主路径示例）；也可换 mysqlclient

## 快速开始

```bash
python -m venv .venv
# Windows
.venv\Scripts\activate
# Linux
# source .venv/bin/activate

pip install -r requirements.txt

# 按实际环境修改（密码仅通过环境变量注入）
export OG_MYSQL_HOST=127.0.0.1
export OG_MYSQL_PORT=3308
export OG_MYSQL_DB=mysql_test_db
export OG_MYSQL_USER=omm
export OG_MYSQL_PASSWORD='your_password_here'

python scripts/run_demo.py
```

## 配置要点

`config/settings.py` 默认使用：

- `ENGINE = django.db.backends.mysql`（原生 MySQL backend）
- `PORT = dolphin_server_port`（如 3308）
- `NAME` = B 库下 schema（MySQL database）
- `PASSWORD` = 环境变量 `OG_MYSQL_PASSWORD`（仓库不保存明文口令）

并通过 `pymysql_bootstrap.py` 将 PyMySQL 注册为 MySQLdb；若 `SELECT VERSION()` 仍是 openGauss banner，则按 dolphin 握手版本解析为 MySQL 8.0.28。

### Plugin 侧依赖

Django 初始化会执行：

```sql
SELECT VERSION(),
       @@sql_mode,
       @@default_storage_engine,
       @@sql_auto_is_null,
       @@lower_case_table_names,
       ...
```

openGauss 本身没有 MySQL 存储引擎概念；在 dolphin（Plugin）中按 `query_cache_size` 同类方式增加兼容桩，使 `@@default_storage_engine` 固定返回 `InnoDB`，`@@sql_auto_is_null` 默认为 `0` 且可 `SET`，从而原生 backend 可正常工作。

未合入该 Plugin 补丁的旧环境，可临时：

```bash
export OG_MYSQL_ENGINE=opengauss_mysql
```

使用本仓库附带的兼容 Backend（仅作过渡）。

## 产出

- 示例工程：本目录
- 设计文档：`doc/设计文档.md`
- 自测报告：`docs/自测报告.md`
- 测试思维导图：`docs/测试思维导图.mm` / `.md` / `.html`
- 官方开发指南：docs 仓 `django_development.md`
- Plugin：`@@default_storage_engine` / `@@sql_auto_is_null` 兼容变量
