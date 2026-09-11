# mysql-connector-python × openGauss(dolphin) 兼容性测试

本目录包含使用 **mysql-connector-python** 连接 openGauss B 兼容模式数据库（dolphin 插件）的兼容性测试代码及自测报告。

## 环境要求

| 依赖 | 版本 |
|------|------|
| openGauss | 7.0.0-LTS |
| dolphin 插件 | Plugin 仓 master 分支 |
| Python | 3.9+ |
| mysql-connector-python | 8.0.33 |

安装驱动：

```bash
pip install mysql-connector-python==8.0.33
```

## 必要连接参数

与标准 MySQL 连接相比，连接 openGauss dolphin 插件时须额外指定以下参数：

```python
import mysql.connector

conn = mysql.connector.connect(
    host='127.0.0.1',
    port=3308,                            # dolphin MySQL 协议端口
    user='your_user',
    password='your_password',
    database='your_schema',
    charset='utf8mb4',
    collation='utf8mb4_general_ci',       # 必须：openGauss 不支持默认的 0900_ai_ci
    auth_plugin='mysql_native_password',  # 必须：不支持 caching_sha2_password
    use_pure=True,                        # 必须：C 扩展认证与 dolphin 不兼容
    sql_mode='NO_BACKSLASH_ESCAPES',      # 必须：openGauss 默认 standard_conforming_strings=on，
                                          # 反斜杠不是转义字符；须通过此参数告知驱动改用引号加倍
                                          # 转义，否则含单引号/反斜杠的字符串参数会报语法错误
)
```

## 运行测试

```bash
python test_connector_opengauss.py
```

预期输出：

```
mysql-connector × openGauss(dolphin) compatibility test
========================================================
  PASS  01 basic connect
  PASS  02 CRUD
  ...
  PASS  18 error type mapping
========================================================
Result: 18 passed, 0 failed / 18 total
```

## 文件说明

| 文件 | 说明 |
|------|------|
| `test_connector_opengauss.py` | 兼容性测试脚本，覆盖 18 个测试项 |
| `self_test_report_connector.md` | 自测报告，含问题分析与测试结果汇总 |

## 已知限制

- **C 扩展不可用**：`use_pure=True` 为必要参数，C 扩展无法通过 dolphin 认证
- **collation**：必须指定 `utf8mb4_general_ci`，`utf8mb4_0900_ai_ci` 不被支持
- **sql_mode 必须在连接时指定**：`sql_mode='NO_BACKSLASH_ESCAPES'` 须作为连接参数传入，连接建立后通过 `SET sql_mode=...` 动态切换无法同步更新驱动侧的转义策略
- **bytes/BLOB 原生绑定的反斜杠限制**：`%s` 绑定 `bytes` 类型时，含反斜杠字节（`0x5C`）会被静默丢失；建议改用 `UNHEX(%s)` 写入、`HEX(data)` 读取的方式处理任意二进制数据
- **null 字节参数**：字符串参数中的 `\x00` 会触发语法错误
- **错误码映射**：异常类型映射不完整，`ProgrammingError`/`IntegrityError` 返回为 `DatabaseError`
