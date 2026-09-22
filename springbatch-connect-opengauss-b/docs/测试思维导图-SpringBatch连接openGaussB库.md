# 测试思维导图：Spring Batch + MySQL JDBC 连接 openGauss B 库（Issue #29）

```text
Spring Batch + MySQL JDBC 连接 openGauss B 库 测试（18 用例 × 8 档驱动 = 144 项，全部通过）
│
├── 测试环境
│   ├── openGauss 7.0.0-RC3（fe6f6e4b，debug）B 兼容库 mysql_test_db
│   ├── MySQL 协议端口 3308（enable_dolphin_proto = on）
│   └── Spring Boot 3.5.16 + Spring Batch 5.2.6 + JDK 17 + mysql-connector-j（BOM 默认 9.7.0）
│
├── 一、元数据表初始化（1 用例 ✓）
│   ├── 框架自带 schema-mysql.sql 自动建 9 张 BATCH_* 表全部成功
│   │   （spring.batch.jdbc.initialize-schema=always，platform=mysql）
│   ├── ENGINE=InnoDB 子句被 B 模式接受 ✓
│   └── TIMESTAMP DEFAULT '0000-00-00 00:00:00' DDL 执行通过
│       （B 模式默认 sql_mode 含 no_zero_date，但 DDL 阶段未触发拒绝）
│
├── 二、作业执行与数据处理（2 用例 ✓）
│   ├── chunk 拷贝转换作业：读 `user`（3 行）→ 名字大写、age+1 → 写 user_batch_out
│   │   ├── chunk=2：3 行切 2 个事务；输出行数 = 源行数 ✓
│   │   └── 元数据核对：BATCH_JOB_INSTANCE / EXECUTION / STEP_EXECUTION / PARAMS 各 +1 ✓
│   ├── 大数据分块：src_large 1000 行 → dst_large，chunk=200
│   │   ├── JdbcCursorItemReader 游标逐行读 + JdbcBatchItemWriter 批量写 ✓
│   │   └── READ_COUNT=WRITE_COUNT=1000；COMMIT_COUNT=6
│   │       （5 个满 chunk + 末次空读触发 1 次空提交，框架行为观测）
│   └── AUTO_INCREMENT 主键的源表 DDL 在 B 模式建表成功（DDL 兼容验证点）✓
│
├── 三、作业生命周期（3 用例 ✓，含 EXIT_MESSAGE 超长场景）
│   ├── 同参数重跑（上次 COMPLETED）→ JobInstanceAlreadyCompleteException ✓
│   │   （identifying 参数参与实例身份判定，与元数据落库一致）
│   ├── 新参数 → 新 JobInstance，BATCH_JOB_INSTANCE +1 ✓
│   └── 说明：失败路径 EXIT_MESSAGE 超长被拒（error 130，与 MySQL 严格模式一致）
│       ├── 框架将完整异常栈（>2500 字符）写入 EXIT_MESSAGE VARCHAR(2500)
│       │   → 被 B 模式拒绝：value too long for type character varying(2500)
│       │   （与 MySQL 严格模式 STRICT_TRANS_TABLES 行为一致）
│       ├── 后果：job / step 元数据停留 STARTED 状态、框架报 unknown state
│       └── 规避：① ALTER 放大 EXIT_MESSAGE 列（补测一实测可行，见下）；② 监听器截断
│           exitDescription；③ 会话级去掉 sql_mode strict 模式（与 MySQL 一致截断）
│
├── 四、容错（2 用例 ✓）
│   ├── skip：2 个坏行（SKIP_ME）跳过、作业 COMPLETED
│   │   READ_COUNT=5 / WRITE_COUNT=3 / PROCESS_SKIP_COUNT=2 ✓
│   └── retry：retryLimit=2（含首次共 2 次总尝试）第 2 次成功
│       ├── 处理器按行计数重试、输出表含该行 ✓
│       └── 观测：retryLimit(n) 语义为总尝试次数而非"额外重试 n 次"
│
├── 五、监听器与执行上下文（1 用例 ✓）
│   ├── JobExecutionListener beforeJob 写入 ExecutionContext、afterJob 读回 ✓
│   └── 观测（5.2.6 实测）：执行上下文以 base64(Java 序列化) 存于
│       SHORT_CONTEXT 列，SERIALIZED_CONTEXT 列为 NULL（读取需 base64 解码）
│
├── 六、作业参数类型（1 用例 ✓）
│   ├── String / Long / Date 三种参数 PARAMETER_TYPE 落库核对
│   │   （java.lang.String / java.lang.Long / java.util.Date）✓
│   └── identifying=false 参数不参与实例身份：仅换该值重跑仍判同实例拒绝 ✓
│
├── 七、多步骤作业（1 用例 ✓）
│   ├── step1（chunk 拷贝）→ step2（tasklet 计数校验）按序执行 ✓
│   ├── BATCH_STEP_EXECUTION 按 STEP_EXECUTION_ID 序落两行 ✓
│   └── step2 写入的上下文持久化（SHORT_CONTEXT 解码后含业务键）✓
│
├── 附加探针（1 用例 ✓，VARCHAR 长度语义佐证）
│   └── VARCHAR 语义探针：VARCHAR(2500) 插入 2496 字符/2502 字节（含汉字）成功
│       → B 模式 VARCHAR(n) 按字符计（与 MySQL 一致），排除字节语义
│       → 确认超长原因在框架写入值本身、非列长度语义差异
│
├── 补测一、失败重启与断点恢复（1 用例 ✓，放大 EXIT_MESSAGE 列宽后实测）
│   ├── 前置：ALTER 放大 BATCH_JOB_EXECUTION / BATCH_STEP_EXECUTION 的 EXIT_MESSAGE
│   │   到 VARCHAR(10000)；6 行源、chunk=2、第 4 行抛错
│   ├── 首轮作业 FAILED：EXIT_MESSAGE 2550 字符完整落库（放大列宽后实测可行）✓
│   ├── JobOperator.restart 复用原参数重启 → COMPLETED、输出 6 行齐 ✓
│   ├── 断点恢复：重启执行 READ=WRITE=4（跳过已提交 2 行续读）、COMMIT=3 ✓
│   ├── 同 JobInstance 复用：实例 1 个、执行 2 个（首败 + 重启成功）✓
│   └── 观测：TaskExecutorJobLauncher.run() 对业务失败不抛异常、
│       返回 FAILED 的 JobExecution（需按状态断言，框架语义）
│
├── 补测二、JdbcPagingItemReader 分页读（1 用例 ✓，含 FINDING B）
│   ├── FINDING B：afterPropertiesSet 调 jdbcTemplate.setMaxRows(pageSize)，
│   │   Connector/J 以 SET sql_select_limit = n 实现 → B 库不识别
│   │   （unrecognized configuration parameter "sql_select_limit"，error 28804；
│   │   MySQL 上正常生效，9.7.0 实测复现 / 26.7.0 字节码证实同一机制）
│   ├── 规避：OpenGaussPagingItemReader 子类初始化后反射归零 JDBC 层 maxRows
│   │   （每页行数已由 SQL 自身 LIMIT 限定，归零不影响分页语义）
│   ├── 规避后全链路：100 行源、pageSize=20、chunk=20
│   │   → SELECT ... ORDER BY id LIMIT ? OFFSET ? 分页 5 页 ✓
│   └── READ=WRITE=100；COMMIT_COUNT=6（5 满页 + 末次空读 1 次空提交）✓
│
├── 补测三、监听器族（1 用例 ✓）
│   ├── StepExecutionListener：before/after 各 1 次，beforeStep 可见步骤名 ✓
│   ├── SkipListener：2 个坏行 onSkipInProcess 按序记录 [SKIP_ME, SKIP_ME] ✓
│   ├── ItemProcessListener：afterProcess 共 5 次、顺序 [good1, good1, good2,
│   │   good2, good3]——带坏行的 chunk 首轮整体失败回滚、重试时好行重新处理、
│   │   坏行跳过不触发；干净 chunk 只处理 1 次（框架重试语义）✓
│   ├── ChunkListener：beforeChunk 每次 chunk 尝试各 1 次（5）、afterChunk 仅
│   │   成功提交触发（3）——失败回滚的尝试不触发 afterChunk ✓
│   └── 元数据印证：ROLLBACK=2 / COMMIT=3 / PROCESS_SKIP=2 / READ=5 / WRITE=3 ✓
│
├── 补测四、条件流（1 用例 ✓）
│   ├── on("FAILED")：flowStepA 置 ExitStatus.FAILED → flowFallbackStep
│   │   （BATCH_STEP_EXECUTION 顺序 [flowStepA, flowFallbackStep]）✓
│   └── on("*")：flowStepA 正常 → flowSuccessStep（顺序 [flowStepA, flowSuccessStep]）✓
│
├── 补测五、@StepScope / @JobScope 晚绑定（1 用例 ✓）
│   ├── @JobScope Step：chunk size 从作业参数 chunkSize=3 晚绑定
│   │   → 9 行切 3 满 chunk，COMMIT_COUNT=4（3 满 + 末次空读空提交）✓
│   ├── @StepScope reader：每个 step 执行时创建（游标每次打开）✓
│   ├── @JobScope tasklet：作业参数 mark=lb-ok 晚绑定写库并断言一致 ✓
│   └── 作用域安全性：启动期不触发 "Scope 'step' is not active"
│       （晚绑定代理直到作业运行才解析）✓
│
├── 补测六、JobExplorer / JobOperator 元数据查询（1 用例 ✓）
│   ├── JobExplorer：getJobInstances（最新在前）/ getJobExecutions /
│   │   步骤执行名（copyUserStep）读路径全部正常 ✓
│   └── JobOperator：getJobNames（JobRegistry 解析）与
│       getStepExecutionSummaries(execId) 步骤摘要正常 ✓
│
├── 驱动版本矩阵（8 档 × 18 用例 = 144 项，全部通过）
│   ├── driver-boot-default（Boot BOM 管理，9.x）：18/18 ✓
│   ├── 8.0.28（旧坐标 mysql:mysql-connector-java）：18/18 ✓
│   ├── 8.0.33 / 8.1.0 / 8.3.0 / 8.4.0：各 18/18 ✓
│   └── 9.7.0 / 26.7.0（新坐标 com.mysql:mysql-connector-j）：各 18/18 ✓
│       → Spring Batch 批处理链路与驱动版本无关，8 档行为一致
│
└── 结论
    ├── Spring Batch 5.2 在 B 库上全链路可用：元数据建表、作业执行、
    │   生命周期、容错、监听器、参数、多步骤、重启恢复、分页读、条件流、
    │   晚绑定均与 MySQL 行为一致
    ├── EXIT_MESSAGE 超长系框架失败路径写长栈 + VARCHAR(2500) 严格校验的组合，
    │   与 MySQL 严格模式一致；放大列宽后实测可行，重启恢复链路已验证
    ├── FINDING B 系 Connector/J 以 SET sql_select_limit 实现 Statement.setMaxRows
    │   与 B 库会话变量缺口的组合；分页读按 OpenGaussPagingItemReader 归零
    │   maxRows 规避（LIMIT 由分页 SQL 自身限定）
    └── 支撑 docs 仓新增《Spring Batch 开发指导》：环境准备 / 项目搭建 /
        示例代码 / 注意事项与已知限制（含 EXIT_MESSAGE 处理与 FINDING B 规避）
```
