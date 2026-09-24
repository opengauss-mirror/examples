# 测试思维导图：SpringDataJPA + MySQL JDBC 连接 openGauss B 库（Issue #28）

> 大纲版思维导图。可视化版见同名 .html，可编辑版见同名 .mm（XMind / FreeMind / MindMaster 均可打开）。

```text
SpringDataJPA + MySQL JDBC 连接 openGauss B 库 测试（19 用例；BOM 默认 9.7.0 基线全通过）
│
├── 测试环境
│   ├── openGauss 7.0.0-RC3（fe6f6e4b，debug）B 兼容库 proto_test_db / schema mysql_test_db
│   ├── MySQL 协议端口 3308（enable_dolphin_proto = on）
│   └── Spring Boot 3.5.16 + Hibernate 6.6.53 + mysql-connector-j 9.7.0（BOM 默认）
│       方言 MySQLDialect（自定义 OpenGaussBMySQLDialect 保留适配点）
│
├── 一、已知差异回归（2 用例 ✓）
│   ├── K1：派生 LIKE（findByNameLike）复现转义语义差异（#504）
│   │   invalid escape string —— Hibernate 6.6 默认追加 escape '\\'
│   │   （两个反斜杠；MySQL 中 '\\'=1 字符），B 模式按标准 SQL 把 '\\'
│   │   视为 2 字符；根源是 B 模式不支持反斜杠作为字符串转义字符
│   │   （与 Plugin 仓 issue #504 同源）
│   ├── K2：JPQL 显式 escape '!'（searchByName）可用 ✓
│   │   workaround 稳定，且与驱动版本无关（8.0.28 与 9.7.0 同行为）
│   └── 方法论：K1 写成回归断言 —— 服务端行为调整后测试自动失败，
│       提醒移除 workaround 并更新文档
│
├── 二、ddl-auto 专项（2 用例）
│   ├── D1：ddl-auto=update 容器启动成功但表未创建 → FINDING E
│   │   HHH000339：information_schema.KEYWORDS 不存在
│   │   （默认配置 dolphin.lower_case_table_names=0 下大小写敏感：小写可查）
│   │   → schema 对比失败 → update 静默失效（不建表也不报错）
│   │   → 实际项目：启动正常，运行时才报 relation does not exist
│   │   → 解法：dolphin.lower_case_table_names=1 并持久化后实测大写 KEYWORDS
│   │     可查、Hibernate 元数据探测恢复
│   └── D2：ddl-auto=validate 未报错（元数据同样缺失，校验深度存疑，如实表述）
│
├── 三、类型映射（1 用例 ✓）
│   └── M1：BigDecimal / LocalDate / LocalDateTime / Boolean↔TINYINT(1) / String 往返
│       + IDENTITY 主键回填（getGeneratedKeys 链路正常）
│
├── 四、分页与批量（2 用例 ✓）
│   ├── P1：PageRequest 多页遍历（LIMIT/OFFSET 与 count 查询正常）
│   └── P2：saveAll(50) 批量主键逐个回填
│
├── 五、冒烟（3 用例 ✓）
│   └── saveAndFind / pagination / transactionRollback（@Transactional 回滚正常）
│
├── 六、协议命令专项（9 用例 ✓，评审意见落地，与 #27 报告 7.1-7.9 同设计）
│   ├── C1 COM_QUERY / C2 COM_INIT_DB / C3 COM_PING ✓
│   ├── C4 COM_STMT_PREPARE / COM_STMT_EXECUTE / COM_STMT_CLOSE
│   │   （useServerPrepStmts=true 全生命周期）✓
│   ├── C5 COM_STMT_SEND_LONG_DATA（服务端预编译 + 1MB LONGBLOB）
│   │   ├── 无零字节 1MB 完整往返（MD5 一致，9.7.0 基线）✓
│   │   ├── FINDING H：8.0.28~8.4.0 档读回仅 1040384 字节（= 127×8192，
│   │   │   丢末块 8KB，MD5 失败；9.7.0/26.7.0 档完整）
│   │   └── FINDING F：含 0x00 二进制被按 C 字符串截断为前缀
│   │       （200 字节读回 150 = 首个 0x00 位置；客户端预编译路径无此问题）
│   ├── C6 COM_STMT_RESET（clearParameters 后复用）✓（8.0.33 及以上档）
│   │   └── FINDING I：8.0.28 档此序列直接崩溃 openGauss 服务端进程
│   │       （EOFException 断连、3308 不可达、可复现 3 次）
│   ├── C7 COM_STMT_FETCH（useCursorFetch + setFetchSize）✓
│   │   驱动层可用、协议层待确认
│   ├── C8 COM_SET_OPTION（allowMultiQueries 多语句）
│   │   └── FINDING G：后续 SELECT 结果集 getMoreResults() 不可得
│   │       （仅首语句结果 + 末 DML update count；DML 均落库）
│   └── C9 COM_QUIT ✓；无 JDBC 触发路径的命令在报告 4.1.6 说明
│
├── 驱动版本矩阵（7 档 × 19 用例）
│   ├── 8.0.28（文档基准）：FINDING I 崩溃 + FINDING H 丢 8KB，
│   │   其余 12 通过（含 K1/D1 复现）
│   ├── 8.0.33 / 8.1.0 / 8.3.0 / 8.4.0：18/19（仅 FINDING H，
│   │   RESET 正常、服务端不崩）
│   ├── 9.7.0（Boot 默认）/ 26.7.0（最新）：19/19 全部通过
│   └── 三档行为：8.0.28 崩 / 8.0.33~8.4.0 丢数据 / 9.7.0+ 正常 →
│       核心链路与 K1/D1/FINDING E/F/G 均与驱动版本无关；
│       FINDING H/I 为服务端缺陷、触发条件依赖驱动版本
│
└── 结论
    ├── JPA 核心链路（类型映射 / 主键回填 / 分页 / 批量 / 事务）可用
    ├── 六个问题/差异根源在服务端：
    │   ① 反斜杠转义语义差异（LIKE escape，已知差异，与 #504 同源）
    │   ② FINDING E information_schema 表名大小写敏感（ddl-auto 失效，
    │      配置 lower_case_table_names=1 持久化可恢复）
    │   ③ FINDING F SEND_LONG_DATA 二进制 0x00 截断
    │   ④ FINDING G 多语句结果返回结构不完整
    │   ⑤ FINDING H SEND_LONG_DATA 1MB 丢末块 8KB（8.0.28~8.4.0 档）
    │   ⑥ FINDING I COM_STMT_RESET 崩溃服务端（仅 8.0.28 档）
    └── 指导文档建议：LIKE 显式 escape '!'；ddl-auto 建议
        lower_case_table_names=1（或 none + 手工建表）；
        服务端预编译 + SEND_LONG_DATA/RESET 场景建议 9.7.0 及以上驱动
```
