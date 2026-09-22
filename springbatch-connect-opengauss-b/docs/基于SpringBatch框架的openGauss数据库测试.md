# 基于SpringBatch框架的openGauss数据库测试

版权所有 © 2026  openGauss社区
 您对“本文档”的复制、使用、修改及分发受知识共享(Creative Commons)署名—相同方式共享4.0国际公共许可协议(以下简称“CC BY-SA 4.0”)的约束。为了方便用户理解，您可以通过访问[*https://creativecommons.org/licenses/by-sa/4.0/*](https://creativecommons.org/licenses/by-sa/4.0/) 了解CC BY-SA 4.0的概要 (但不是替代)。CC BY-SA 4.0的完整协议内容您可以访问如下网址获取：[*https://creativecommons.org/licenses/by-sa/4.0/legalcode*](https://creativecommons.org/licenses/by-sa/4.0/legalcode)。

修订记录

| 日期      | 修订版本 | 修改描述                                        | 作者 |
| --------- | -------- | ----------------------------------------------- | ---- |
| 2026-09-20 | 1.0     | openGauss 对 Spring Batch + MySQL JDBC 的兼容性测试（12 用例 × 8 档驱动矩阵 96 项全部通过；1 项行为说明与 3 项框架行为观测） | lele-2026 |
| 2026-09-20 | 1.1     | 按对话评审补充 6 个 Spring Batch 特性覆盖缺口并全部验证：失败重启与断点恢复（放大 EXIT_MESSAGE 列宽后 2550 字符异常栈落库、重启续跑）、分页读（新增 FINDING B：SET sql_select_limit 被 B 库拒绝 error 28804 及规避）、监听器族、条件流、晚绑定、JobExplorer/JobOperator；用例扩至 18 个 × 8 档驱动矩阵 144 项全部通过 | lele-2026 |

[TOC]

**关键词**：

Spring Batch，Spring Boot，MySQL JDBC，openGauss 数据库，B 兼容模式，dolphin 插件，兼容性测试，批处理

**Abstract 摘要**：

本测试验证使用 MySQL JDBC 驱动（mysql-connector-j）连接 openGauss B 兼容模式数据库，在 Spring Batch 5.2 框架（基于 Spring Boot 3.5）下的兼容性。测试覆盖元数据表初始化、作业执行与数据处理、作业生命周期、容错（skip/retry）、监听器与执行上下文、作业参数类型、多步骤作业七个维度 11 个用例与附加探针 1 个用例共 12 个，并扫描 8 档驱动（BOM 默认档 + 7 个显式版本，8.0.28 ~ 26.7.0）验证版本边界；v1.1 按对话评审补充 Spring Batch 特性覆盖缺口 6 个用例（失败重启与断点恢复、JdbcPagingItemReader 分页读、监听器族、条件流、@StepScope/@JobScope 晚绑定、JobExplorer/JobOperator 元数据查询），共 18 个用例。结果显示：Spring Batch 5.2 批处理全链路（元数据建表、chunk 分块处理、作业生命周期、容错、监听器、参数类型、多步骤、重启恢复、分页读、条件流、晚绑定）在 openGauss B 模式下均可正常工作，8 档驱动矩阵 144 项全部通过，批处理链路行为与驱动版本无关；共记录 1 项 FINDING（B：JDBC 驱动将 Statement.setMaxRows 实现为 SET sql_select_limit 会话变量，B 库不识别该变量（error 28804），JdbcPagingItemReader 分页读需规避）、1 项行为说明（EXIT_MESSAGE 声明为 VARCHAR(2500)，失败栈超长时报 error 130，与 MySQL 严格模式一致）以及 3 项框架行为观测（VARCHAR(n) 按字符计与 MySQL 一致；执行上下文以 base64(Java 序列化) 存于 SHORT_CONTEXT、SERIALIZED_CONTEXT 为 NULL；末次空读触发 1 次空提交）。

**缩略语清单：**

| 缩略语 | 英文全名                    | 中文解释       |
| ------ | --------------------------- | -------------- |
| JDBC   | Java Database Connectivity  | Java 数据库连接 |
| DDL    | Data Definition Language    | 数据定义语言   |
| DML    | Data Manipulation Language  | 数据操作语言   |
| BOM    | Bill of Materials           | 依赖物料清单   |
| ETL    | Extract-Transform-Load      | 抽取转换装载（批处理典型场景） |

***

# 1 概述

本报告针对 Spring Batch 框架配合 MySQL JDBC 驱动与 openGauss B 兼容模式数据库的兼容性测试活动进行总结。openGauss 的 dolphin 插件实现了 MySQL 协议兼容，用户在服务端开启 `enable_dolphin_proto` 后，可通过 MySQL 的 JDBC 驱动直接连接 openGauss B 库。

本测试的测试对象为 Spring Batch 5.2.6 框架（基于 Spring Boot 3.5.16 自动装配，内嵌 mysql-connector-j 9.7.0 驱动）与 openGauss 数据库（B 兼容模式）的交互，另以 mysql-connector-java 8.0.28 作为官方文档声明的基线版本对照。测试聚焦批处理典型场景：框架元数据表初始化（BATCH_* 9 表）、chunk 分块读写、作业生命周期与重启语义、skip/retry 容错、监听器与执行上下文持久化、作业参数类型、多步骤编排，并以 VARCHAR 长度语义探针辅助 FINDING 定位；v1.1 补充失败重启与断点恢复、JdbcPagingItemReader 分页读（LIMIT/OFFSET 方言）、监听器族（Step/Chunk/Skip/ItemProcess）、条件流（ExitStatus 分支）、@StepScope/@JobScope 晚绑定、JobExplorer/JobOperator 元数据查询六个特性覆盖。

# 2 测试版本说明

## 2.1 测试版本信息

### 2.1.1 被测版本

| 版本名称   | 软件包名称                    | 测试起始时间 | 测试结束时间 | 测试人员 |
| ---------- | ----------------------------- | ------------ | ------------ | -------- |
| Spring Boot | spring-boot-starter-batch 3.5.16（Spring Batch 5.2.6） | 2026-09-19 | 2026-09-20 | lele-2026 |
| mysql-connector-j | 9.7.0（Boot BOM 默认） | 2026-09-19 | 2026-09-20 | lele-2026 |
| openGauss  | openGauss 7.0.0-RC3（源码编译，commit fe6f6e4b） | 2026-09-19 | 2026-09-20 | lele-2026 |

> 说明：实测环境为 openGauss 7.0.0-RC3（与 issue 开发指导文档同版本）。
>
> 说明：被测二进制基于 7.0.0-RC3（commit fe6f6e4b）的本地源码工作副本编译：dolphin 插件为本地 contrib/dolphin 副本，contrib/timescaledb 未参与编译。上述差异均不在本测试覆盖的批处理元数据表 / JDBC 游标读 / 批量写 / 事务提交路径上，不影响测试结论。

### 2.1.2 配套测试的版本

| 版本名称 | 配套版本        | 版本说明                                        |
| -------- | --------------- | ----------------------------------------------- |
| JDK      | 17.0.15（Liberica） | Spring Boot 3.x 要求 JDK 17+                |
| Maven    | 3.9.14          | 构建工具                                        |
| 驱动矩阵 | 8 档（BOM 默认档 + 7 个显式版本） | BOM 默认档（Boot 3.5.16 管理，解析为 9.7.0）/ 8.0.28（旧坐标）/ 8.0.33 / 8.1.0 / 8.3.0 / 8.4.0 / 9.7.0（显式）/ 26.7.0，用于版本边界验证 |

## 2.2 测试环境描述

### 2.2.1 环境硬件信息

| 环境信息 | 硬件配置信息                                                 | 备注              |
| -------- | ------------------------------------------------------------ | ----------------- |
| 测试执行环境 | JDK 17.0.15、Maven 3.9.14 | 运行测试用例 |
| 数据库环境 | openEuler 20.03 LTS（x86_64）<br />openGauss 源码编译安装（debug） | 被测数据库 |

### 2.2.2 OS版本

| 操作系统  | OS版本      | 版本说明                 |
| --------- | ----------- | ------------------------ |
| openEuler | 20.03 (LTS) | x86_64 版本              |

### 2.2.4 数据库环境配置

| 配置项                        | 值                                        |
| ----------------------------- | ----------------------------------------- |
| 协议开关                      | `enable_dolphin_proto = on`               |
| MySQL 协议端口                | `dolphin_server_port = 3308`              |
| 默认数据库                    | `dolphin.default_database_name = 'mysql_test_db'` |
| 测试数据库                    | `mysql_test_db`（DBCOMPATIBILITY 'B'，schema mysql_test_db） |
| 测试用户                      | `mysql_test_db`（已执行 `set_native_password` 原生密码认证） |
| 客户端接入认证                | pg_hba.conf：`host all mysql_test_db 0.0.0.0/0 sha256` |
| 防火墙                        | firewalld 放行 3308/tcp                   |
| 测试表                        | `` `user` ``（id INT AUTO_INCREMENT PK, name VARCHAR(50), age INT），预置 3 行（含 SKIP_ME / RETRY_ME / FAIL_ME 三种故障行）；`user_batch_out`（作业输出表，每次启动重建）；`BATCH_*` 9 张元数据表（框架 schema-mysql.sql 自动初始化）；`src_large` / `dst_large`（大数据分块用例自建） |

# 3 版本概要测试结论、关键风险和规避措施

## 3.1 测试结论总结

本测试共设计 18 个测试用例：七个维度 11 个（元数据表初始化 1、作业执行与数据处理 2、作业生命周期 3、容错 2、监听器与执行上下文 1、作业参数类型 1、多步骤作业 1）+ 附加探针 1 个 + v1.1 对话评审补充 6 个（失败重启与断点恢复 1、分页读 1、监听器族 1、条件流 1、晚绑定 1、JobExplorer/JobOperator 1）。**18 个用例全部通过**（"通过"= 行为与断言一致；差异行为以 FINDING 记录），8 档驱动矩阵 144 项执行全部通过。

核心结论：

1. Spring Batch 5.2（Boot 3.x BOM 管理的 mysql-connector-j 新坐标）配合 openGauss B 模式，框架元数据表初始化、chunk 分块处理、作业生命周期、skip/retry 容错、监听器与执行上下文持久化、作业参数类型、多步骤编排、失败重启与断点恢复、分页读、条件流、作用域晚绑定、元数据查询 API 均可正常工作；
2. **批处理链路行为与驱动版本无关**：同一套 18 用例扫描 8 档驱动（8.0.28 ~ 26.7.0，覆盖新旧两套 Maven 坐标）144 项全部通过，各档行为一致（详见 4.2）；
3. 记录 1 项兼容性差异（FINDING B：SET sql_select_limit 被拒 error 28804，分页读需规避，详见 3.3、4.1.10、5.2）；另记录 1 项行为说明（EXIT_MESSAGE 声明为 VARCHAR(2500)，失败栈超长时报 error 130，与 MySQL 严格模式一致，详见 4.1.9）与 3 项框架行为观测（VARCHAR 按字符计、SHORT_CONTEXT 存 base64 序列化、末次空读触发空提交，详见 4.1）。

## 3.2 约束说明

1. 本测试覆盖 Spring Batch 单实例典型批处理场景，未覆盖远程分区（remote partitioning）、远程分块（remote chunking）、K8s 平台部署、JobRepository 多实例并发（集群锁行为）等分布式特性；
2. 测试基于 openGauss 7.0.0-RC3；
3. 失败重启与断点恢复链路在放大 EXIT_MESSAGE 列宽的前提下验证通过（见 4.1.9）；
4. 批量吞吐为功能级验证（1000 行、chunk=200），未做基准测试；本次未实测 caching_sha2_password 认证方式与 TLS/SSL 连接（需服务端 SSL 配置），版本范围结论仅针对 mysql_native_password 认证与 JDBC 驱动，不覆盖 MySQL 命令行客户端。

## 3.3 关键风险和规避措施

| 风险 | 说明 | 规避措施 |
| ---- | ---- | -------- |
| EXIT_MESSAGE 声明长度限制（行为说明项） | 该列声明为 VARCHAR(2500)，失败路径框架写入的异常栈超过该长度即报 `value too long for type character varying(2500)`（error 130），与 MySQL 严格模式 STRICT_TRANS_TABLES 一致；该轮 job/step 元数据停留 STARTED | 如需保留完整失败信息：放大该列（如 `EXIT_MESSAGE VARCHAR(10000)`，job/step 两表同理，实测可行），或在 afterJob 中截断 exitDescription |
| FINDING B：JdbcPagingItemReader 分页读初始化失败 | 框架在 `JdbcPagingItemReader.afterPropertiesSet()` 调用 `jdbcTemplate.setMaxRows(pageSize)`；Connector/J 将该语义实现为执行 `SET sql_select_limit = n` 会话变量，B 库不识别该变量（`unrecognized configuration parameter "sql_select_limit"`，error 28804），导致分页读初始化失败（BOM 默认档 9.7.0 实测复现；26.7.0 字节码证实同一机制；MySQL 上该变量正常生效，系 B 库会话变量缺口） | 示例工程以 `OpenGaussPagingItemReader` 子类规避：`afterPropertiesSet()` 后经反射把基类私有 JdbcTemplate 的 maxRows 归零，不再触发 `SET sql_select_limit`——每页行数已由查询 SQL 自身 `LIMIT` 限定，JDBC 层 maxRows 仅为冗余保险，归零不影响分页语义（已在 4.1.10 实测验证，8 档驱动全矩阵通过） |
| 观测：执行上下文读取需解码 | 框架（5.2.6 实测）将 ExecutionContext 以 base64(Java 序列化) 存于 SHORT_CONTEXT 列，SERIALIZED_CONTEXT 列为 NULL（与 MySQL 上行为一致，系框架版本行为） | 需在 SQL 侧读取业务键时对 SHORT_CONTEXT 做 base64 解码（Java 侧用 `Base64.getDecoder().decode()`）；或通过 JobExplorer / StepExecution API 读取 |

# 4 版本详细测试结论

## 4.1 特性测试结论

### 4.1.1 元数据表初始化（1 用例，全部通过）

| 用例 | 结果 |
| ---- | ---- |
| 框架 schema-mysql.sql 自动建表（`spring.batch.jdbc.initialize-schema=always`，platform=mysql） | ✓ BATCH_JOB_INSTANCE / BATCH_JOB_EXECUTION / BATCH_JOB_EXECUTION_PARAMS / BATCH_JOB_EXECUTION_CONTEXT / BATCH_JOB_EXECUTION_SEQ / BATCH_STEP_EXECUTION / BATCH_STEP_EXECUTION_CONTEXT / BATCH_STEP_EXECUTION_SEQ / BATCH_JOB_SEQ 共 9 表全部创建成功 |
| DDL 兼容点：`ENGINE=InnoDB` 子句 | ✓ 被 B 模式接受（建表语句中的 MySQL 引擎子句未报错） |
| DDL 兼容点：`TIMESTAMP DEFAULT '0000-00-00 00:00:00'` | ✓ DDL 执行通过（B 模式默认 sql_mode 含 no_zero_date，但零日期默认值在 DDL 阶段未触发拒绝；与 #27 FINDING B 的 DML 写入拒绝行为不冲突） |

### 4.1.2 作业执行与数据处理（2 用例，全部通过）

| 用例 | 结果 |
| ---- | ---- |
| chunk 拷贝转换作业（读 `` `user` `` → 名字大写、age+1 → 写 user_batch_out，chunk=2） | ✓ 作业 COMPLETED：输出行数 = 源行数（3）；JOIN 核对 `o.age = u.age + 1 AND o.name = UPPER(u.name)` 逐行成立；chunk=2 将 3 行切为 2 个事务提交 |
| 元数据核对 | ✓ 每轮运行 BATCH_JOB_INSTANCE / BATCH_JOB_EXECUTION / BATCH_STEP_EXECUTION 各 +1；BATCH_JOB_EXECUTION_PARAMS 按参数逐项落行（PARAMETER_NAME=run.id 行数 +1） |
| 大数据分块（src_large 1000 行 → dst_large，chunk=200） | ✓ JdbcCursorItemReader 游标逐行读 + JdbcBatchItemWriter 批量写完整链路：输出 1000 行；READ_COUNT=WRITE_COUNT=1000 |
| 提交计数（观测） | ✓ COMMIT_COUNT=6：5 个满 chunk（200×5）+ 末次空读触发 1 次空提交（框架行为，非 B 库差异） |
| DDL 兼容点：AUTO_INCREMENT 主键 | ✓ `CREATE TABLE src_large (id INT AUTO_INCREMENT PRIMARY KEY, ...)` 在 B 模式建表与自增取值均正常（1000 行主键连续） |

### 4.1.3 作业生命周期（3 用例）

| 用例 | 结果 |
| ---- | ---- |
| 同参数重跑（上次 COMPLETED） | ✓ 抛 `JobInstanceAlreadyCompleteException`：identifying 参数（run.id）参与实例身份判定，与元数据落库一致（作业不可重复执行语义在 B 库上成立） |
| 新参数 → 新 JobInstance | ✓ BATCH_JOB_INSTANCE 数量 +1，同名作业按参数区分实例 |
| 失败路径 EXIT_MESSAGE 写入（说明） | ✗（说明项）处理器抛 RuntimeException 且无容错时，框架把完整异常栈写入 EXIT_MESSAGE（VARCHAR(2500)）；栈超长时报 `value too long for type character varying(2500)`（error 130），与 MySQL 严格模式行为一致，该轮 job/step 状态停留 STARTED。放大列宽后写入与重启恢复均正常（见 4.1.9） |

### 4.1.4 容错（2 用例，全部通过）

| 用例 | 结果 |
| ---- | ---- |
| skip（faultTolerant + skip(IllegalArgumentException)，skipLimit=100） | ✓ 3 行源中含 2 个坏行（SKIP_ME）被跳过、作业 COMPLETED；BATCH_STEP_EXECUTION 核对 READ_COUNT=5 / WRITE_COUNT=3 / PROCESS_SKIP_COUNT=2（B 库上计数列读写与 MySQL 一致） |
| retry（faultTolerant + retry(IllegalStateException)，retryLimit=2） | ✓ 第 1 次失败、第 2 次成功，作业 COMPLETED、输出表含该行；处理器按行计数验证实际尝试 2 次 |
| retry 语义（观测） | ✓ retryLimit(n) 为**总尝试次数**（含首次）而非"额外重试 n 次"——retryLimit=2 时共执行 2 次（框架语义，非 B 库差异） |

### 4.1.5 监听器与执行上下文（1 用例，全部通过）

| 用例 | 结果 |
| ---- | ---- |
| JobExecutionListener beforeJob / afterJob | ✓ beforeJob 向 Job ExecutionContext 写入 `listener.marker`，afterJob 读回并对外可见（作业级回调时序正常） |
| 作业级上下文落库 | ✓ BATCH_JOB_EXECUTION_CONTEXT 存在对应行；SHORT_CONTEXT base64 解码后含 `listener.marker` |
| 步骤级上下文落库 | ✓ BATCH_STEP_EXECUTION_CONTEXT 存在对应行；解码后含框架写入的 `batch.stepType` |
| SHORT_CONTEXT 存储格式（观测） | ✓ 框架（5.2.6 实测）将 ExecutionContext 以 base64(Java 序列化) 存于 SHORT_CONTEXT 列，SERIALIZED_CONTEXT 列为 NULL——SQL 侧读取业务键需先 base64 解码（与 MySQL 上行为一致，系框架版本行为） |

### 4.1.6 作业参数类型（1 用例，全部通过）

| 用例 | 结果 |
| ---- | ---- |
| 三种参数类型落库 | ✓ 依次以 addString / addLong / addDate 运行后，BATCH_JOB_EXECUTION_PARAMS.PARAMETER_TYPE 分别为 java.lang.String / java.lang.Long / java.util.Date，PARAMETER_VALUE 与传入值一致 |
| identifying=false 参数 | ✓ `addString("run.hint", ..., identifying=false)` 后 IDENTIFYING 列为 'N'；仅换 hint 值重跑（identifying 参数 run.id 不变）仍抛 JobInstanceAlreadyCompleteException——非识别参数不参与实例身份，语义与 MySQL 一致 |

### 4.1.7 多步骤作业（1 用例，全部通过）

| 用例 | 结果 |
| ---- | ---- |
| 两步顺序执行（step1 chunk 拷贝 → step2 tasklet 计数校验） | ✓ 作业 COMPLETED、共 2 个 StepExecution；step2 tasklet 内部断言输出表非空（证明 step1 先行提交），并写入 `step2.saw.rows` 键 |
| 步骤元数据顺序 | ✓ BATCH_STEP_EXECUTION 按 STEP_EXECUTION_ID 序为 ["step1Copy", "step2CountTasklet"] 两行 |
| 步骤上下文持久化 | ✓ BATCH_STEP_EXECUTION_CONTEXT 中 step2 上下文 SHORT_CONTEXT 解码后含 `step2.saw.rows` |

### 4.1.8 附加探针（1 用例，全部通过）

| 用例 | 结果 |
| ---- | ---- |
| VARCHAR(2500) 长度语义探针 | ✓ 插入 2500 字符纯 ASCII 成功；插入 2496 字符/2502 字节（含 6 个汉字，多 12 字节）同样成功 → **B 模式 VARCHAR(n) 按字符计（与 MySQL 一致），非字节语义** |

### 4.1.9 失败重启与断点恢复（1 用例，全部通过）

| 用例 | 结果 |
| ---- | ---- |
| 首轮中途失败（放大 EXIT_MESSAGE 列宽后） | ✓ 先 `ALTER TABLE BATCH_JOB_EXECUTION / BATCH_STEP_EXECUTION MODIFY EXIT_MESSAGE VARCHAR(10000)`；6 行源数据、chunk=2、第 4 行（RESTART_FAIL）抛错 → 首轮作业 FAILED；EXIT_MESSAGE 落库长度 2550 字符（完整异常栈），两表状态均为 FAILED（未放大前此处写入会报 error 130，与 MySQL 严格模式一致） |
| JobOperator.restart 断点续跑 | ✓ 关闭故障开关后 `jobOperator.restart(execId)` 复用首轮参数重启 → 作业 COMPLETED，输出表 6 行齐；重启执行 READ_COUNT=WRITE_COUNT=4（跳过首轮已提交的 2 行、从第 3 行续读）、COMMIT_COUNT=3——断点恢复语义成立（chunk 提交粒度） |
| 同 JobInstance 复用 | ✓ 同一参数下 BATCH_JOB_INSTANCE 1 个、BATCH_JOB_EXECUTION 2 个（首败 + 重启成功），与框架 MySQL 语义一致 |
| 框架语义（观测） | ✓ `TaskExecutorJobLauncher.run()` 对作业执行失败不抛异常（仅对启动期错误如 JobInstanceAlreadyComplete 抛），而是返回 FAILED 状态的 JobExecution——业务失败需按返回状态断言（框架语义，非 B 库差异） |

### 4.1.10 JdbcPagingItemReader 分页读（1 用例，全部通过，含 FINDING B）

| 用例 | 结果 |
| ---- | ---- |
| FINDING B | ✗（记录）`JdbcPagingItemReader.afterPropertiesSet()` 调用 `jdbcTemplate.setMaxRows(pageSize)`，Connector/J 将其实现为 `SET sql_select_limit = n`，B 库不识别（error 28804），分页读初始化失败（MySQL 上正常生效） |
| FINDING B 规避后分页读全链路 | ✓ 用 `OpenGaussPagingItemReader` 子类在初始化后把 JDBC 层 maxRows 归零（每页行数已由 SQL 自身 LIMIT 限定）→ 100 行源数据、pageSize=20、chunk=20，框架按 MySQL 方言生成 `SELECT ... ORDER BY id LIMIT ? OFFSET ?`：输出 100 行、READ_COUNT=WRITE_COUNT=100、COMMIT_COUNT=6（5 满页 + 末次空读 1 次空提交） |
| B 库 LIMIT/OFFSET 解析兼容点 | ✓ 分页 SQL 的 LIMIT/OFFSET 参数绑定在 B 库全部正常（游标读之外的又一条 DB 读取路径） |

### 4.1.11 监听器族（1 用例，全部通过）

| 用例 | 结果 |
| ---- | ---- |
| StepExecutionListener | ✓ beforeStep / afterStep 各触发 1 次，beforeStep 可见步骤名 moreListenersStep |
| SkipListener | ✓ 5 行源数据含 2 个坏行（SKIP_ME）、chunk=2 + skip(IllegalArgumentException)：onSkipInProcess 按处理顺序记录 2 次 SKIP_ME |
| ItemProcessListener（含重试语义） | ✓ afterProcess 共触发 5 次、顺序 [good1, good1, good2, good2, good3]：带坏行的 chunk 首次处理整体失败回滚，TaskletStep 重试该 chunk 时好行**重新处理**（afterProcess 再次触发）、坏行被 skip；干净 chunk（good3）仅处理 1 次；坏行从不触发 afterProcess——B 库上计数与 MySQL 一致 |
| ChunkListener | ✓ beforeChunk 每次 chunk 尝试各触发 1 次（2 个带坏行 chunk 各重试 1 次 + 1 个干净 chunk = 5 次）、afterChunk 仅在 chunk 成功提交时触发（3 次）——失败回滚的尝试不触发 afterChunk（框架语义） |
| 元数据核对 | ✓ ROLLBACK_COUNT=2 / COMMIT_COUNT=3 / PROCESS_SKIP_COUNT=2 / READ_COUNT=5 / WRITE_COUNT=3 与监听器计数互相印证 |

### 4.1.12 条件流（1 用例，全部通过）

| 用例 | 结果 |
| ---- | ---- |
| on("FAILED") 分支 | ✓ flowStepA tasklet 把 ExitStatus 置为 FAILED → 流程路由到 flowFallbackStep；作业 COMPLETED（分支终点不改变作业状态）；BATCH_STEP_EXECUTION 顺序 [flowStepA, flowFallbackStep] |
| on("*") 分支 | ✓ flowStepA 正常退出 → 路由到 flowSuccessStep；顺序 [flowStepA, flowSuccessStep] |
| 语义核对 | ✓ 条件流按 ExitStatus 分支（而非仅顺序 .next()），JobBuilder.on()/from()/to()/end() 编排在 B 库上行为正常 |

### 4.1.13 @StepScope / @JobScope 晚绑定（1 用例，全部通过）

| 用例 | 结果 |
| ---- | ---- |
| chunk size 晚绑定 | ✓ @JobScope Step 从作业参数 chunkSize 取 3 → 9 行切 3 个满 chunk，COMMIT_COUNT=4（3 满 chunk + 末次空读 1 次空提交）；作业参数 chunkSize=3 已落库于 BATCH_JOB_EXECUTION_PARAMS |
| @StepScope reader | ✓ @StepScope JdbcCursorItemReader 每个 step 执行时创建（游标每次打开），作业 COMPLETED、输出 9 行 |
| @JobScope tasklet 参数绑定 | ✓ lbMarkStep 的 tasklet 从作业参数 mark 取 "lb-ok" 写入 dst_lb_mark 表并断言一致 |
| 作用域安全性（观测） | ✓ @JobScope Step + @StepScope reader 的组合在作业启动期不触发 "Scope 'step' is not active"（晚绑定代理直到作业运行才解析） |

### 4.1.14 JobExplorer / JobOperator 元数据查询（1 用例，全部通过）

| 用例 | 结果 |
| ---- | ---- |
| JobExplorer 读路径 | ✓ 跑一次 copyUserJob 后：getJobInstances 返回实例列表（最新在前）、getJobExecutions 返回执行及步骤执行（含 copyUserStep 名）——对 BATCH_* 表的读 API 全部正常 |
| JobOperator 读路径 | ✓ getJobNames()（JobRegistry 解析）含 copyUserJob；getStepExecutionSummaries(execId) 返回步骤摘要（含 copyUserStep） |

## 4.2 兼容性测试结论（驱动版本矩阵）

同一套 18 用例扫描 8 档驱动（8 个 Maven profile 切换，同一服务端环境；其中 BOM 默认档与显式 9.7.0 为同一版本的两条管理路径）：

| 驱动版本 | 坐标 | 结果 |
| -------- | ---- | ---- |
| 8.0.28（文档基准） | mysql:mysql-connector-java（旧坐标） | 18/18 通过 |
| 8.0.33 | com.mysql:mysql-connector-j（新坐标） | 18/18 通过 |
| 8.1.0 | 同上 | 18/18 通过 |
| 8.3.0 | 同上 | 18/18 通过 |
| 8.4.0 | 同上 | 18/18 通过 |
| 9.7.0（Boot 3.5.16 BOM 默认档） | com.mysql:mysql-connector-j（版本由 BOM 管理） | 18/18 通过 |
| 9.7.0（显式 pin） | 同上 | 与 BOM 默认档同一版本同一 jar（仅 Maven 管理路径不同），18/18 通过 |
| 26.7.0（最新） | 同上 | 18/18 通过 |

8 档驱动的元数据建表、chunk 处理、生命周期、容错计数、监听器上下文、参数类型、多步骤、重启恢复、分页读（FINDING B 规避后）、条件流、晚绑定行为以及 EXIT_MESSAGE 超长时的报错表现（error 130）**逐项一致**。

**结论：Spring Batch 批处理链路（元数据表、游标读、批量写、事务提交、容错、上下文持久化）与驱动版本无关，8 档（8.0.28 ~ 26.7.0，新旧两套 Maven 坐标）均可正常工作。** 结合 #27/#28 测试结论，官方文档的版本范围表述可由"MySQL JDBC driver：8.0.28 及其以下"更新为"MySQL JDBC 驱动 8.0.28 及以上版本在 Spring 生态（Boot / JPA / Batch）场景均可正常工作"（对应 docs 仓指导文档，仅针对 JDBC 驱动表述；MySQL 命令行客户端未实测）。

# 5 测试对象质量评估

## 5.1 覆盖率分析

| 覆盖维度 | 覆盖情况 |
| -------- | -------- |
| 功能覆盖 | 批处理七维度 11 用例 + 探针 1 用例：元数据表初始化（9 表 + 2 个 DDL 兼容点）/ 作业执行与数据处理（chunk 拷贝转换、大数据分块、元数据核对）/ 作业生命周期（同参数重跑、新实例、失败路径）/ 容错（skip、retry）/ 监听器与执行上下文 / 作业参数类型（3 类 + identifying）/ 多步骤作业；附加 VARCHAR 长度语义探针。v1.1 补充 6 用例：失败重启与断点恢复（JobOperator.restart、断点续跑、同 JobInstance 复用）/ JdbcPagingItemReader 分页读（LIMIT/OFFSET 方言、FINDING B 规避）/ 监听器族（StepExecution/Chunk/Skip/ItemProcess 同作业联动）/ 条件流（on("FAILED")/on("*") ExitStatus 分支）/ @StepScope+@JobScope 晚绑定（chunk size、reader、tasklet 参数）/ JobExplorer+JobOperator 元数据查询 |
| 版本覆盖 | 驱动 8 档（BOM 默认档 + 7 个显式版本，8.0.28 ~ 26.7.0，覆盖新旧两套 Maven 坐标） |
| 未覆盖项 | 远程分区/远程分块（remote partitioning/chunking）、JobRepository 多实例并发与集群锁、K8s 部署、高并发批量吞吐基准、caching_sha2_password 认证、TLS/SSL 连接、MySQL 命令行客户端、分区并行步骤（Partitioner）、JobOperator 运行期管理动作（stop/abandon，仅覆盖 restart） |

## 5.2 缺陷统计和分析

### 5.2.1 缺陷统计

| 类别 | 数量 | 说明 |
| ---- | ---- | ---- |
| 服务端差异（FINDING） | 1 | B：SET sql_select_limit 会话变量不被 B 库识别（error 28804），JdbcPagingItemReader 分页读需规避（4.1.10）；非崩溃类缺陷 |
| 行为说明 | 1 | EXIT_MESSAGE 声明为 VARCHAR(2500)，失败栈超长时报 error 130，与 MySQL 严格模式一致（4.1.9）；非缺陷 |
| 崩溃类缺陷 | 0 | 无 |
| 框架行为观测 | 3 | ① VARCHAR(n) 按字符计（与 MySQL 一致）；② 执行上下文存 base64(Java 序列化) 于 SHORT_CONTEXT、SERIALIZED_CONTEXT 为 NULL；③ 末次空读触发 1 次空提交（COMMIT_COUNT=6）；均非缺陷 |

### 5.2.2 缺陷列表

| 编号 | 现象 | 定位 | 严重程度 | 规避措施 |
| ---- | ---- | ---- | -------- | -------- |

| FINDING B | JdbcPagingItemReader 初始化报 error 28804（`unrecognized configuration parameter "sql_select_limit"`），分页读不可用 | 框架 `afterPropertiesSet()` 调用 `jdbcTemplate.setMaxRows(pageSize)`；Connector/J 将该语义实现为 `SET sql_select_limit = n`（26.7.0 字节码证实 StatementImpl.executeInternal 无条件 setSessionMaxRows），B 库不识别该会话变量（MySQL 上正常生效） | 中（影响分页读这一条标准读取路径；游标读不受影响） | 初始化后把 JDBC 层 maxRows 归零（示例工程 `OpenGaussPagingItemReader` 经反射重置基类私有 JdbcTemplate），每页行数由查询 SQL 自身 LIMIT 限定；或改用 JdbcCursorItemReader |

# 6 测试过程评估

## 6.1 测试策略回顾

采用"控制变量 + 矩阵扫描"策略：先以 Boot BOM 默认驱动（9.7.0）跑全七维度 12 用例建立基线，再以 Maven profile 切换驱动版本扫描矩阵（8 档全量回归）；对每一项与 MySQL 官方行为的差异，用最小复现定位差异归属（框架侧 or 服务端侧）——FINDING B 即通过"原始分页读报 error 28804 → javap 字节码确认 Connector/J setSessionMaxRows 机制 → 子类归零 maxRows 复测通过"的路径定位。v1.1 补充的 6 个用例在对话评审指出的覆盖缺口上补测，全部纳入矩阵扫描。

## 6.2 测试设计评估

1. 按 Spring Batch 批处理核心能力切分七个维度：元数据表初始化 → 作业执行与数据处理 → 生命周期 → 容错 → 监听器与执行上下文 → 参数类型 → 多步骤，每维度用独立测试类与独立作业 bean，互不干扰、可单独复测；v1.1 补充的 6 个用例沿用同一结构（独立测试类 + 独立作业 bean + 自建自删数据表）；
2. 元数据表初始化同时验证了 2 个 DDL 兼容点（ENGINE=InnoDB 子句、零日期默认值），与 #27 FINDING B（零日期 DML 写入受 no_zero_date 控制）形成互补口径；
3. 每个用例除业务断言外均核对 BATCH_* 元数据（计数列、参数表、上下文），把"框架是否真实落库"纳入断言，而非只看作业状态；监听器族用例进一步用"监听器计数 ↔ 元数据计数"双向印证（ROLLBACK_COUNT=2 ↔ 重试 2 次等）；
4. VARCHAR 探针固化列长度语义（按字符计）；失败重启用例覆盖"失败 → 放大 EXIT_MESSAGE 列宽 → 重启 → 断点续跑"全链路；
5. 观测项（SHORT_CONTEXT base64 格式、COMMIT_COUNT 空提交、retryLimit 语义、afterProcess 重试触发语义、TaskExecutorJobLauncher 失败不抛异常）均为框架行为，已在报告中注明"非 B 库差异"，避免误报；
6. 分页读用例因 FINDING B 使用规避子类（OpenGaussPagingItemReader），用例同时验证了规避方案本身在 8 档驱动矩阵下的可用性。

## 6.3 测试执行评估

### 6.3.1 测试执行统计数据

| 项目 | 数据 |
| ---- | ---- |
| 用例总数 | 18（七维度 11 + 探针 1 + 补充 6） |
| 驱动档位 | 8 档（BOM 默认档 + 7 个显式版本；含文档基准 8.0.28 与最新 26.7.0） |
| 执行轮次 | 8 档各一轮全量回归 + 若干轮单用例复测（FINDING B 定位、监听器重试语义确认等） |
| 总执行用例次 | 18 × 8 = 144 |

### 6.3.2 测试用例执行结果统计数据

| 结果 | 数量 |
| ---- | ---- |
| 通过 | 144（8 档各 18/18） |
| 失败 | 0 |
| 错误 | 0 |

# 7 附件

## 7.1 附件1：相关产物

| 产物 | 位置 |
| ---- | ---- |
| 测试代码（18 用例：七维度 11 + 探针 1 + 补充 6） | 本目录 `src/test/java/org/opengauss/batch/`（SchemaInitTest / SmokeTest / LargeDataTest / JobLifecycleTest / RestartTest / SkipRetryTest / ListenerTest / ParamTypesTest / MultiStepTest / VarcharProbeTest + RestartResumeTest / PagingReaderTest / MoreListenersTest / ConditionalFlowTest / LateBindingTest / ExplorerOperatorTest）；补充作业与规避类见 `src/main/java/org/opengauss/batch/GapJobsConfig.java`、`OpenGaussPagingItemReader.java` |
| 测试思维导图 | 本目录 `docs/测试思维导图-SpringBatch连接openGaussB库.md` |
| 指导文档 | docs 仓 `docs/zh/mysql_compatibility_developer_guide/springbatch_development.md`（轻量版 `docs-lite/zh/...` 同路径） |
