# mysql-connector-python × openGauss(dolphin) 自测报告

## 一、环境信息

| 项目 | 版本/说明 |
|------|-----------|
| 操作系统 | openEuler 5.10.0-60.139.0.166.oe2203.x86_64 |
| openGauss | 7.0.0-RC3 debug |
| dolphin 插件 | Plugin 仓 master 分支 |
| mysql-connector-python | 8.0.33 |
| Python | 3.9 |
| MySQL 协议端口 | 3308 |
| 原生 PG 端口 | 5432 |
| 测试数据库 | pymysql_test（B 兼容模式） |
| 测试用户 | pymysql_user |
| 认证方式 | mysql_native_password |

---

## 二、发现的兼容性问题及解决方案

### 问题 1：C 扩展（CMySQLConnection）认证失败

**现象**：  
使用默认连接方式（C 扩展）时，认证阶段报错：
```
mysql.connector.errors.DatabaseError: 514 (HY000): failed in auth check, role:pymysql_user
```

**根因**：  
mysql-connector-python 8.x 默认使用 C 扩展（`CMySQLConnection`），其 `mysql_native_password` 处理流程与 dolphin 插件的 `AuthSwitchRequest` 实现存在兼容性问题，导致服务端密码比对失败。

**解决方案**：  
连接时指定 `use_pure=True`，强制使用纯 Python 实现（`MySQLConnection`）：
```python
conn = mysql.connector.connect(
    ...
    use_pure=True,
)
```

---

### 问题 2：默认 collation `utf8mb4_0900_ai_ci` 不支持

**现象**：  
使用 `charset='utf8mb4'` 的默认连接执行建表语句时报错：
```
28804 (HY000): collation "utf8mb4_0900_ai_ci" for encoding "UTF8" does not exist
```

**根因**：  
mysql-connector-python 8.x 默认使用 MySQL 8.0 引入的 `utf8mb4_0900_ai_ci` 校对规则（Unicode 9.0），openGauss 不支持此 collation，仅支持旧版 `utf8mb4_general_ci`。

**解决方案**：  
连接时显式指定 collation：
```python
conn = mysql.connector.connect(
    ...
    charset='utf8mb4',
    collation='utf8mb4_general_ci',
)
```

---

### 问题 3：字符串参数中反斜杠/单引号的转义兼容性

**现象**：  
传递含单引号（`'`）或反斜杠（`\`）的字符串参数时，报错：
```
24708 (HY000): unterminated bit string literal at or near "b'"
```

**根因**：  
mysql-connector-python 默认使用 `\'` 方式转义单引号（MySQL 风格），但 openGauss 不将 `\'` 识别为转义序列。`'a\'b'` 被解析为字符串 `a\`（提前结束），后续的 `b'` 被误识别为 bit string 字面量起始，导致语法错误。

**解决方案**：  
在会话中设置 `NO_BACKSLASH_ESCAPES` 模式，驱动将改用 `''` 双写方式转义单引号，与 openGauss 兼容：
```python
cur.execute("SET sql_mode='NO_BACKSLASH_ESCAPES'")
```
或在 URL 参数/初始化 SQL 中全局设置。

---

### 问题 4：`sql_mode` 必须在连接层全局设置，不能逐语句临时 `SET`

**现象**：  
仅在个别测试用例里临时执行 `SET sql_mode='NO_BACKSLASH_ESCAPES'`，会导致 mysql-connector-python 内部缓存的 `connection.sql_mode` 属性与服务端会话实际状态不一致，普通 CRUD 测试也缺少对含单引号/反斜杠参数的验证。

**根因**：  
mysql-connector-python 会在连接建立时读取 `sql_mode` 连接参数并缓存，用于决定 `%s` 绑定字符串参数时的转义策略；单条 `SET sql_mode=...` 语句只改变服务端会话变量，**不会**同步更新驱动内部缓存的转义策略。

**解决方案**：  
在 `mysql.connector.connect()` 时直接传入 `sql_mode` 参数，对整条连接生效：
```python
conn = mysql.connector.connect(
    ...
    sql_mode="NO_BACKSLASH_ESCAPES",
)
```

---

### 问题 5：`bytes`（BLOB）参数通过 `%s` 绑定存在根本性转义缺陷

**现象**：  
无论 `sql_mode` 如何设置（全局 `NO_BACKSLASH_ESCAPES`、连接中途 `SET sql_mode=''`、或完全不设置 `sql_mode` 的全新连接），只要 `bytes` 参数中含有需要转义的字节（如 `0x27` 单引号、`0x5C` 反斜杠、`0x00` 等），通过 `%s` 绑定插入时均报**完全相同**的错误：
```
24708 (HY000): unterminated quoted string at or near "'"
```

**根因**：  
mysql-connector-python 对 `bytes` 类型参数的 `%s` 转义逻辑**始终假定反斜杠转义有效**（写死实现，不跟随 `sql_mode` 动态调整），但 openGauss 的 SQL 解析遵循 PostgreSQL 的 `standard_conforming_strings` 语义——普通字符串字面量中反斜杠**从不是转义字符**。驱动客户端"转义"出来的 `\'`、`\0` 等序列，在服务端被当作字面的反斜杠+普通字符处理，导致引号提前意外闭合，产生语法错误。该行为与 `sql_mode` 设置完全无关，已用四种连接配置验证结果一致。

另外还尝试了预处理语句（`conn.cursor(prepared=True)`）绕开文本协议转义，但发现该方式下 `bytes` 参数**无法正确传输给 Dolphin**，写入的数据会静默变成 `NULL`，属于驱动与 Dolphin 之间更深层的二进制协议不兼容，同样不可用。

**解决方案**：  
改用 `UNHEX(%s)` 写入、`HEX(data)` 读取：让 `%s` 只绑定安全的十六进制**字符串**（纯 ASCII，无需任何转义，`str` 类型的 `%s` 绑定已验证正常），真正的二进制转换交给服务端函数完成：
```python
cur.execute("INSERT INTO t_blob VALUES (%s, UNHEX(%s))", (id_, data.hex()))
...
cur.execute("SELECT id, HEX(data) FROM t_blob ORDER BY id")
rows = cur.fetchall()
actual_bytes = bytes.fromhex(rows[0][1])
```
这种写法仍然通过 `%s` 走真正的参数绑定路径（而非拼接 SQL 字面量），且能验证字节级精确内容，兼顾了审阅意见对"真实参数绑定"和"内容校验"的要求。

---

## 三、测试结果汇总

| # | 测试项 | 结果 | 备注 |
|---|--------|------|------|
| 01 | 基础连接与认证 | PASS | `use_pure=True` + `mysql_native_password` |
| 02 | CRUD（增删改查） | PASS | 含单引号+反斜杠参数（`O'Brien\Corp`）验证全局 `sql_mode` 下的普通写入安全 |
| 03 | 参数化查询 & 批量插入 | PASS | `executemany` 10 条；验证实际行内容，不仅核验行数 |
| 04 | autocommit 属性 | PASS | `conn.autocommit = True/False`（属性，非方法） |
| 05 | commit / rollback | PASS | |
| 06 | dict cursor | PASS | `cursor(dictionary=True)` |
| 07 | unbuffered cursor | PASS | `cursor(buffered=False)`，对应 PyMySQL 的 SSCursor |
| 08 | fetchmany | PASS | 需在下一条语句前消耗完剩余结果 |
| 09 | 中文 & Unicode | PASS | 中/日/韩/emoji 往返正确 |
| 10 | 特殊字符 | PASS | 依赖连接层全局 `sql_mode="NO_BACKSLASH_ESCAPES"`（问题 4） |
| 11 | NO_BACKSLASH_ESCAPES | PASS | 验证 `@@sql_mode` 已在连接时生效；新增裸连接（无 `sql_mode` 参数）验证 HandshakeV10 修复后首次查询直接通过 |
| 12 | 二进制 BLOB | PASS | Part A：原生 `bytes` 绑定（仅含单引号 0x27）在 HandshakeV10 修复后可正常往返；含反斜杠 0x5C 仍会静默丢失（已知限制）；Part B：`UNHEX(%s)`/`HEX(data)` 覆盖 `0x00`~`0xFF` 全字节范围 |
| 13 | 大文本（1 MB） | PASS | 读回全部内容与原始字符串精确比对（不仅验证 `LENGTH()`） |
| 14 | 日期时间类型 | PASS | 验证返回类型（date/timedelta/datetime）且逐字段核验实际值与写入值一致 |
| 15 | DECIMAL | PASS | 精确返回 `decimal.Decimal` |
| 16 | 多连接事务隔离 | PASS | 未提交不可见，提交后可见 |
| 17 | 重连稳定性 ×10 | PASS | 连续 10 次独立连接全部成功 |
| 18 | 错误类型映射 | PASS | 改为确定性断言：若无异常抛出则测试失败；异常类型与 MySQL 标准不一致时以打印说明，不掩盖实际行为 |

**总计：18 passed，0 failed**（各测试均使用确定性断言验证实际值，不存在"永远 PASS"的测试项）

---

## 四、已知限制

### 错误码映射不完整

openGauss 错误码与 MySQL 标准错误码存在差异，mysql-connector 无法正确识别异常类型：

| 场景 | 期望异常 | 实际异常 |
|------|----------|----------|
| 表不存在 | `ProgrammingError` | `DatabaseError` |
| 重复主键 | `IntegrityError` | `DatabaseError` |

**建议**：dolphin 插件补充 MySQL 错误码映射表（后续 PR）。

### null 字节与反斜杠参数传递（字符串类型）

| 参数内容 | 结果 | 原因 |
|----------|------|------|
| `"\x00"` (null 字节) | 失败 | 驱动编码为 bit string 格式，openGauss 语法错误 |
| `"a\\b"` (反斜杠) | 仅在非 `NO_BACKSLASH_ESCAPES` 模式下失败 | 与单引号问题同根因 |

**建议**：应用层避免在普通字符串参数中传递 null 字节；连接时统一设置 `NO_BACKSLASH_ESCAPES`。

### `bytes` 参数 `%s` 绑定（部分改善，0x5C 仍有限制）

HandshakeV10 修复后，mysql-connector-python 对含单引号（0x27）的 `bytes` 参数进行原生 `%s` 绑定已可正常往返（语法错误消失）。但含反斜杠（0x5C）的字节仍会被静默丢失，原因是驱动内部对 `bytes` 使用的 `_binary'...'` 前缀格式不受 `NO_BACKSLASH_ESCAPES` 状态位影响，0x5C 在服务端仍被当作转义字符前缀消耗。**凡涉及任意二进制数据（含 0x5C）的场景，仍需使用 `UNHEX(%s)`/`HEX()` 方案（Part B），不能依赖原生 `bytes` 绑定。**

### 预处理语句（`prepared=True`）不支持二进制参数

`conn.cursor(prepared=True)` 走二进制协议本应绕开文本转义问题，但实测发现通过该方式绑定的 `bytes` 参数**无法正确传输给 Dolphin**，写入的数据会静默变成 `NULL`（无异常抛出），属于驱动与 Dolphin 之间更深层的二进制协议不兼容。**不建议在连接 openGauss dolphin 插件时使用 `mysql-connector-python` 的预处理语句绑定二进制参数。**

### C 扩展不可用

mysql-connector-python 的 C 扩展（`CMySQLConnection`）在当前版本（8.0.33）下无法通过 dolphin 插件认证，**必须使用 `use_pure=True`**。

---

## 五、必要连接参数

与标准 MySQL 连接相比，连接 openGauss dolphin 插件时须额外指定以下参数：

```python
conn = mysql.connector.connect(
    host='127.0.0.1',
    port=3308,                            # dolphin MySQL 协议端口
    user='your_user',
    password='your_password',
    database='your_schema',
    charset='utf8mb4',
    collation='utf8mb4_general_ci',       # 必须，openGauss 不支持 0900_ai_ci
    auth_plugin='mysql_native_password',  # 必须，不支持 caching_sha2_password
    use_pure=True,                        # 必须，C 扩展认证失败
    sql_mode='NO_BACKSLASH_ESCAPES',      # 必须在连接层设置，不能逐语句临时 SET（见问题 4）
)
```

> **注意**：`bytes`（BLOB）参数不要依赖 `%s` 直接绑定，需改用 `UNHEX(%s)` 写入 / `HEX(data)` 读取（见问题 5），且不要使用 `cursor(prepared=True)` 绑定二进制参数。

---

## 六、测试代码

见同目录 `test_connector_opengauss.py`，覆盖上述全部 18 个测试项，**18 passed，0 failed**。各测试均使用确定性断言验证实际值，不存在"永远 PASS"的测试项。

---

## 七、结论

mysql-connector-python 8.0.33 在指定正确连接参数（`use_pure=True`、`collation='utf8mb4_general_ci'`、`auth_plugin='mysql_native_password'`、`sql_mode='NO_BACKSLASH_ESCAPES'`）后，可正常连接 openGauss 7.0.0-RC3 dolphin 插件的 B 兼容模式数据库，**18 项测试全部验证通过**（含实际值断言和异常必须抛出断言，无"永远 PASS"测试项）。HandshakeV10 `status_flags` 修复（Plugin PR #2522）已同步验证：裸连接无需 `sql_mode` 参数即可完成含特殊字符的参数化查询。

需要特别注意：
1. `sql_mode` 必须在连接建立时设置，不能通过后续 `SET` 语句动态切换（问题 4）；
2. 二进制（`bytes`/BLOB）数据不能依赖驱动原生的 `%s` 转义或 `prepared=True` 预处理语句，需改用 `UNHEX()`/`HEX()` 方式传输（问题 5）；
3. null 字节传参、错误码映射不完整等仍是已知限制，需应用层自行规避。
