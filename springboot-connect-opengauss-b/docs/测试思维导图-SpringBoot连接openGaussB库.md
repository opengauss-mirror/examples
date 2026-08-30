# 测试思维导图：Spring Boot + MySQL JDBC 连接 openGauss B 库（Issue #27）

> 大纲版思维导图。可视化版见同名 .html，可编辑版见同名 .mm（XMind / FreeMind / MindMaster 均可打开）。

```text
Spring Boot + MySQL JDBC 连接 openGauss B 库 测试（37 用例；BOM 默认 9.7.0 基线全通过）
│
├── 测试环境
│   ├── openGauss 7.0.0-RC3（fe6f6e4b，debug）B 兼容库 proto_test_db / schema mysql_test_db
│   ├── MySQL 协议端口 3308（enable_dolphin_proto = on）
│   └── Spring Boot 3.5.16 + JDK 17 + mysql-connector-j（BOM 默认 9.7.0）
│
├── 一、连接与认证（4 用例 ✓）
│   ├── 正常连接；productName = MySQL，productVersion = 8.0.28-dophin-server
│   ├── 错误密码 → 拒绝（failed in auth check）✓
│   ├── 错误用户名 → 拒绝（no pg_hba.conf entry）✓
│   ├── 连 5432 原生口 → 拒绝（Communications link failure）✓
│   └── FINDING A：DatabaseMetaData.getTables 报错
│       information_schema.TABLES 不存在（默认配置 dolphin.lower_case_table_names=0
│       下大小写敏感：小写可查；设置 =1 并持久化后实测 getTables 恢复正常）
│
├── 二、类型映射 + 6 个文档参数（8 用例 ✓）
│   ├── 9 种类型往返：TINYINT / TINYINT(1)→Boolean / BIT(1)→Boolean /
│   │   DECIMAL→BigDecimal / DATE / DATETIME / TIMESTAMP / VARCHAR / BLOB
│   │   （服务端将 TINYINT(1) 与 BIT(1) 均上报为 typeName=BIT）
│   ├── tinyInt1isBit=false → 返回 Integer ✓（参数生效）
│   ├── transformedBitIsBoolean=false/true → getObject 均映射 Boolean ✓
│   │   （与官方文档备注"Java 侧恒为 Boolean"一致）
│   ├── FINDING B：零日期由 dolphin.sql_mode 的 no_zero_date 控制
│   │   ├── 默认开启 → "0000-00-00" 写入被拒（date/time field value out of range）
│   │   ├── 去掉 no_zero_date → 可正常存储
│   │   └── 存储后 zeroDateTimeBehavior 三档读取均生效（与 MySQL 语义一致）：
│   │       convertToNull→null / exception→抛 Zero date value prohibited / round→0001-01-01
│   ├── 原 FINDING C 改写为正向确认：useOldAliasMetadataBehavior ✓
│   │   表列别名（SELECT name AS n）：true 时 getColumnName 返回别名 n，
│   │   默认返回原始列名 name（参数生效）；
│   │   表达式别名（SELECT 1 AS my_alias）：两种设置均返回别名（符合文档备注）
│   ├── useaffectedrows=true/默认：无变化 UPDATE 均返回 1（找到行数）✓
│   │   （与官方文档备注"目前不支持，始终返回找到行数"一致；
│   │   MySQL 原生 =true 时应返回 0）
│   └── allowMultiQueries=true → 两条语句两个结果集均返回 ✓（参数生效）；
│       默认 → 服务端明确拒绝（Multi-statement is not allowed）
│
├── 三、DML 批量（3 用例 ✓）
│   ├── PreparedStatement.addBatch 1000 行 ✓
│   ├── JdbcTemplate.batchUpdate ✓
│   └── rewriteBatchedStatements=true 提速约 17 倍
│       （500 行 59ms vs 1020ms）→ 建议指导文档推荐开启
│
├── 四、事务（5 用例 ✓）
│   ├── TransactionTemplate 提交后可见 / setRollbackOnly 回滚后不可见 ✓
│   ├── 手动 Connection commit / rollback ✓
│   ├── SAVEPOINT 保存点 + 部分回滚 ✓
│   └── FINDING D：setTransactionIsolation(SERIALIZABLE)
│       读回 4 = REPEATABLE_READ（服务端映射/回落）
│
├── 五、连接池（2 用例 ✓）
│   ├── Boot 默认 HikariCP，maximumPoolSize=10 配置生效 ✓
│   └── CONNECTION_ID() 两次取连接返回同一 id → 池复用确认 ✓
│
├── 六、已知限制专项（3 用例）
│   ├── 游标：useCursorFetch=true + useServerPrepStmts=true + setFetchSize(100)
│   │   驱动层无报错、完整读回 2000 行；协议层是否真游标待确认（如实记录）
│   ├── Statement.setFetchSize(50) 无报错（可能被静默忽略）
│   └── 存储过程 CallableStatement 报错
│       information_schema.PARAMETERS 不存在（默认配置大小写敏感所致；
│       lower_case_table_names=1 后大写可查；存储过程本身文档不支持）
│
├── 七、协议命令专项（9 用例 ✓，评审意见落地）
│   ├── 7.1 COM_QUERY / 7.2 COM_INIT_DB / 7.3 COM_PING ✓
│   ├── 7.4 COM_STMT_PREPARE / COM_STMT_EXECUTE / COM_STMT_CLOSE
│   │   （useServerPrepStmts=true 全生命周期）✓
│   ├── 7.5 COM_STMT_SEND_LONG_DATA（服务端预编译 + 1MB LONGBLOB）
│   │   ├── 无零字节 1MB 完整往返（MD5 一致，9.7.0 基线）✓
│   │   ├── FINDING H：8.0.28~8.4.0 档读回仅 1040384 字节（= 127×8192，
│   │   │   丢末块 8KB，MD5 失败；9.7.0/26.7.0 档完整）
│   │   └── FINDING F：含 0x00 二进制被按 C 字符串截断为前缀
│   │       （200 字节读回 150 = 首个 0x00 位置；客户端预编译路径无此问题）
│   ├── 7.6 COM_STMT_RESET（clearParameters 后复用）✓（8.0.33 及以上档）
│   │   └── FINDING I：8.0.28 档此序列直接崩溃 openGauss 服务端进程
│   │       （EOFException 断连、3308 不可达、可复现 3 次）
│   ├── 7.7 COM_STMT_FETCH（useCursorFetch + setFetchSize）✓
│   │   驱动层可用、协议层待确认
│   ├── 7.8 COM_SET_OPTION（allowMultiQueries 多语句）
│   │   └── FINDING G：后续 SELECT 结果集 getMoreResults() 不可得
│   │       （仅首语句结果 + 末 DML update count；DML 均落库）
│   └── 7.9 COM_QUIT ✓；无 JDBC 触发路径的命令在报告 4.1.8 说明
│
├── 驱动版本矩阵（8 档 × 37 用例）
│   ├── 8.0.28（文档基准，旧坐标 mysql-connector-java）
│   │   └── FINDING I：COM_STMT_RESET 崩溃服务端；FINDING H：丢 8KB；
│   │       其余 30 用例通过（协议类 2 通过 + 1 失败 + 6 错误）
│   ├── 8.0.33 / 8.1.0 / 8.3.0 / 8.4.0（新坐标 mysql-connector-j）
│   │   └── 各档 36/37：仅 FINDING H（SEND_LONG_DATA 丢末块 8KB）失败，
│   │       RESET 正常、服务端不崩
│   ├── 9.7.0（Boot 3.5.16 BOM 默认）/ 26.7.0（最新）
│   │   └── 37/37 全部通过（FINDING H/I 均不触发）
│   └── 三档行为：8.0.28 崩 / 8.0.33~8.4.0 丢数据 / 9.7.0+ 正常 →
│       基础功能与驱动版本无关，协议命令专项与驱动版本相关（缺陷在服务端）
│
└── 结论
    ├── Spring Boot 3.x + 新驱动（≥8.0.31 新坐标）基础 CRUD / 事务 /
    │   批量 / 类型映射 / 连接池均可正常工作
    ├── 服务端预编译 + SEND_LONG_DATA/RESET 场景建议 9.7.0 及以上驱动
    │   （FINDING H/I，详见报告 4.2/5.2.2）
    └── 支撑 docs 仓版本范围表述更新：
        "MySQL JDBC 驱动 8.0.28 及以上版本均可正常工作"
```
