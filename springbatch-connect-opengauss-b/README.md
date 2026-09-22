# Spring Batch 连接 openGauss B 兼容库（Issue #29）

Spring Batch 5.2.x + MySQL JDBC 驱动 -> openGauss dolphin 协议（3308）-> B 库。

## 前置条件

- openGauss B 库 `mysql_test_db`（`enable_dolphin_proto = on`，`dolphin_server_port = 3308`）
- 用户 `mysql_test_db`（已 `set_native_password`）
- schema `mysql_test_db`、表 `` `user` ``（id INT AUTO_INCREMENT PK / name VARCHAR(50) / age INT）
- JDK 17 + Maven 3.9（Spring Batch 运行所需依赖）

## 运行方式

连接信息用环境变量覆盖（默认值见 application.yml）：

```bash
export DB_HOST=<openGauss 机器 IP>  # 默认 127.0.0.1
export DB_PORT=3308
export DB_NAME=mysql_test_db
export DB_USER=mysql_test_db
export DB_PASSWORD=ChangeMe_123     # 占位，替换为实际密码
```

驱动版本扫测矩阵（与 #27/#28 同套 8 档，18 用例全量回归）：

```bash
mvn test -Pdriver-boot-default   # Boot BOM 管理（9.x）
mvn test -Pdriver-8.0.28
mvn test -Pdriver-8.0.33
mvn test -Pdriver-8.1.0
mvn test -Pdriver-8.3.0
mvn test -Pdriver-8.4.0
mvn test -Pdriver-9.7.0
mvn test -Pdriver-26.7.0
```

命令行跑批（可选，测试内默认用 JobLauncher 手动启动）：

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.batch.job.enabled=true
```

## 工程结构

| 文件 | 作用 |
|---|---|
| `CopyUserJobConfig` | 示例作业 copyUserJob：读 `` `user` `` -> 名字转大写、age+1 -> 写 `user_batch_out`，chunk=2 |
| `ExtraJobsConfig` | 维度作业：skipJob / retryJob / failJob / listenerJob / twoStepJob / largeJob |
| `GapJobsConfig` | 补测作业：restartJob（失败重启断点恢复）/ pagingJob（分页读）/ moreListenersJob（监听器族）/ conditionalFlowJob（条件流）/ lateBindingJob（@StepScope/@JobScope 晚绑定） |
| `OpenGaussPagingItemReader` | JdbcPagingItemReader 子类：初始化后反射归零 JDBC 层 maxRows，规避 FINDING B（SET sql_select_limit 不被 B 库识别） |
| `DataInitRunner` | 每次启动重建输出表（DROP IF EXISTS + CREATE 本身是 DDL 兼容性验证点） |
| `src/test/java/org/opengauss/batch/` | 18 用例：元数据表初始化 / 作业执行与数据处理 / 生命周期 / 容错 / 监听器与执行上下文 / 参数类型 / 多步骤 / varchar 语义探针 + 重启恢复 / 分页读 / 监听器族 / 条件流 / 晚绑定 / JobExplorer·JobOperator |

BATCH_* 元数据表由框架自带 `schema-mysql.sql` 在每次启动时自动重建
（`spring.batch.jdbc.initialize-schema=always`）——在 B 库上执行这份 DDL 是本任务第一道兼容性测试。

## 常见问题

- **启动报 `Invalid default value for 'START_TIME'`**：schema-mysql.sql 使用
  `TIMESTAMP DEFAULT '0000-00-00 00:00:00'`，而 B 模式默认 sql_mode 含 `no_zero_date`。
  实测（7.0.0-RC3）DDL 执行通过、未触发该报错；如遇新版行为差异，解法二选一：
  ① 连接 URL 加 `sessionVariables=sql_mode=` 去掉 no_zero_date（适配方式）；
  ② 保留默认行为并记录为 FINDING（报告方式）。
- **作业失败时报 `value too long for type character varying(2500)`（error 130）**：
  `EXIT_MESSAGE` 列声明为 VARCHAR(2500)，失败时框架写入的异常栈超过该长度即被拒绝，
  与 MySQL 严格模式（STRICT_TRANS_TABLES）行为一致。如需保留完整失败信息，可放大该列
  （如 `ALTER TABLE BATCH_JOB_EXECUTION MODIFY EXIT_MESSAGE VARCHAR(10000)`，STEP 表同理），
  或在 `afterJob` 中截断 exitDescription。
- **JdbcPagingItemReader 报 `unrecognized configuration parameter "sql_select_limit"`（error 28804）**：
  分页读取器初始化时调 `jdbcTemplate.setMaxRows(pageSize)`，Connector/J 用
  `SET sql_select_limit = n` 实现该语义，B 库不识别该会话变量。规避：用
  `OpenGaussPagingItemReader`（初始化后反射归零 JDBC 层 maxRows，每页行数已由分页 SQL
  自身 LIMIT 限定），或改用 `JdbcCursorItemReader` 游标逐行读。详见自测报告 FINDING B。
- **执行上下文查不到业务键**：Spring Batch 5.2 把 ExecutionContext 以
  base64(Java 序列化) 存于 `SHORT_CONTEXT` 列，`SERIALIZED_CONTEXT` 列为 NULL
  （框架行为观测，见报告）。读取需 base64 解码。
