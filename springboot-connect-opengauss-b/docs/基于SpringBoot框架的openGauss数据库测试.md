# 基于SpringBoot框架的openGauss数据库测试

版权所有 © 2026  openGauss社区
 您对“本文档”的复制、使用、修改及分发受知识共享(Creative Commons)署名—相同方式共享4.0国际公共许可协议(以下简称“CC BY-SA 4.0”)的约束。为了方便用户理解，您可以通过访问[*https://creativecommons.org/licenses/by-sa/4.0/*](https://creativecommons.org/licenses/by-sa/4.0/) 了解CC BY-SA 4.0的概要 (但不是替代)。CC BY-SA 4.0的完整协议内容您可以访问如下网址获取：[*https://creativecommons.org/licenses/by-sa/4.0/legalcode*](https://creativecommons.org/licenses/by-sa/4.0/legalcode)。

修订记录

| 日期      | 修订版本 | 修改描述                                        | 作者 |
| --------- | -------- | ----------------------------------------------- | ---- |
| 2026-08-28 | 1.0     | openGauss 对 Spring Boot + MySQL JDBC 的兼容性测试 | lele-2026 |
| 2026-08-29 | 1.1     | 用例扩充至 28（补测 useaffectedrows / allowMultiQueries）；FINDING C 改写为参数生效的正向确认；修正 transformedBitIsBoolean 默认值表述 | lele-2026 |
| 2026-08-30 | 1.2     | 用例统计口径修正（六维度 25 + 冒烟 3）；驱动矩阵明确 8 档 = BOM 默认档 + 7 个显式版本 | lele-2026 |
| 2026-09-04 | 1.3     | 协议命令专项 9 用例实测结果回填（新增 FINDING F/G）；FINDING A/B 定位修正（information_schema 大小写敏感为默认配置行为、`dolphin.lower_case_table_names=1` 可解；零日期由 `dolphin.sql_mode` 的 `no_zero_date` 控制、三档 zeroDateTimeBehavior 均生效），与 docs 仓指导文档口径对齐 | lele-2026 |
| 2026-09-04 | 1.4     | 驱动矩阵全量补测：协议命令行为按驱动版本分三档（新增 FINDING H/I）——SEND_LONG_DATA 1MB 丢末块 8KB（8.0.28~8.4.0 档）、COM_STMT_RESET 崩溃服务端进程（仅 8.0.28 档，可复现 3 次）；9.7.0/26.7.0 档 37/37 全绿；矩阵结论由"行为逐项一致"改写为三档行为 | lele-2026 |
| 2026-09-04 | 1.5     | 删除 7.1 遗留问题列表（内部跟踪事项不再在公开报告列出，各 FINDING 的现状与规避已完整记录于 3.3/5.2；与 #28 报告同步处理）；附件重编号；环境表移除内网 IP；附件表移除"原始日志备存"行 | lele-2026 |

[TOC]

**关键词**：

Spring Boot，MySQL JDBC，openGauss 数据库，B 兼容模式，dolphin 插件，兼容性测试

**Abstract 摘要**：

本测试验证使用 MySQL JDBC 驱动（mysql-connector-j）连接 openGauss B 兼容模式数据库，在 Spring Boot 3.x 框架下的兼容性。测试覆盖连接与认证、类型映射、DML 批量、事务、连接池、已知限制、协议命令七个维度 34 个用例与 Spring Boot 冒烟 3 个用例共 37 个，并扫描 8 档驱动（BOM 默认档 + 7 个显式版本，8.0.28 ~ 26.7.0）验证版本边界。结果显示：Spring Boot 3.x 配合 MySQL JDBC 驱动在 openGauss B 模式下基础 CRUD、事务、批量、类型映射、连接池场景在 8 档驱动下均可正常工作；协议命令专项存在两个与驱动版本相关的服务端缺陷（8.0.28 档 COM_STMT_RESET 崩溃服务端、8.0.28~8.4.0 档 SEND_LONG_DATA 丢末块 8KB，9.7.0 起正常）；共记录 7 项 FINDING（A：information_schema 表名大小写敏感，默认配置所致、`dolphin.lower_case_table_names=1` 可解；B：零日期由 `dolphin.sql_mode` 的 `no_zero_date` 控制；D：隔离级别映射；F：COM_STMT_SEND_LONG_DATA 二进制参数遇 0x00 截断；G：多语句结果返回结构不完整；H：SEND_LONG_DATA 1MB 丢末块 8KB，8.0.28~8.4.0 档；I：COM_STMT_RESET 崩溃服务端进程，仅 8.0.28 档），并针对官方协议兼容文档列出的 6 个 JDBC 参数逐一实测（3 项验证生效，3 项行为与文档声明一致，其中 zeroDateTimeBehavior 三档读取行为实测均生效），已在报告中给出规避措施。

**缩略语清单：**

| 缩略语 | 英文全名                    | 中文解释       |
| ------ | --------------------------- | -------------- |
| JDBC   | Java Database Connectivity  | Java 数据库连接 |
| CRUD   | Create/Read/Update/Delete   | 增删改查       |
| DDL    | Data Definition Language    | 数据定义语言   |
| DML    | Data Manipulation Language  | 数据操作语言   |
| BOM    | Bill of Materials           | 依赖物料清单   |

***

# 1 概述

本报告针对 Spring Boot 框架配合 MySQL JDBC 驱动与 openGauss B 兼容模式数据库的兼容性测试活动进行总结。openGauss 的 dolphin 插件实现了 MySQL 协议兼容，用户在服务端开启 `enable_dolphin_proto` 后，可通过 MySQL 的 JDBC 驱动直接连接 openGauss B 库。

本测试的测试对象为 Spring Boot 3.5.16 框架（内嵌 mysql-connector-j 9.7.0 驱动）与 openGauss 数据库（B 兼容模式）的交互，另以 mysql-connector-java 8.0.28 作为官方文档声明的基线版本对照。

# 2 测试版本说明

## 2.1 测试版本信息

### 2.1.1 被测版本

| 版本名称   | 软件包名称                    | 测试起始时间 | 测试结束时间 | 测试人员 |
| ---------- | ----------------------------- | ------------ | ------------ | -------- |
| Spring Boot | spring-boot-starter-jdbc 3.5.16 | 2026-08-28 | 2026-08-29 | lele-2026 |
| mysql-connector-j | 9.7.0（Boot BOM 默认） | 2026-08-28 | 2026-08-29 | lele-2026 |
| openGauss  | openGauss 7.0.0-RC3（源码编译，commit fe6f6e4b） | 2026-08-28 | 2026-08-29 | lele-2026 |

> 说明：任务要求基于 openGauss 7.0.0-LTS，实测环境为 7.0.0-RC3（与 issue 开发指导文档同版本），二者在 MySQL 协议兼容行为上应一致；正式版发布后建议回归。
>
> 说明：被测二进制基于 7.0.0-RC3（commit fe6f6e4b）的本地源码工作副本编译：dolphin 插件为本地 contrib/dolphin 副本，包含此前 round() 支持 blob 任务的本地改动（已核验二进制中存在 round(binary/blob) 系列函数签名），contrib/timescaledb 未参与编译。上述差异均不在本测试覆盖的 MySQL 协议 / JDBC / 类型映射 / 事务路径上，不影响测试结论。

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
| 开发机   | OS：Windows 11<br />JDK 17.0.15<br />Maven 3.9.14            | 测试代码执行环境  |
| 虚拟机   | OS：openEuler 20.03 LTS（x86_64）<br />openGauss 源码编译安装（debug） | 数据库环境        |

### 2.2.2 虚拟化平台

| 虚拟化平台         | 版本说明 |
| ------------------ | -------- |
| VMware Workstation Pro | NAT 网络模式，Windows 开发机可直连虚拟机 |

### 2.2.3 OS版本

| 操作系统  | OS版本      | 版本说明                 |
| --------- | ----------- | ------------------------ |
| openEuler | 20.03 (LTS) | x86_64 版本              |

### 2.2.4 数据库环境配置

| 配置项                        | 值                                        |
| ----------------------------- | ----------------------------------------- |
| 协议开关                      | `enable_dolphin_proto = on`               |
| MySQL 协议端口                | `dolphin_server_port = 3308`              |
| 默认数据库                    | `dolphin.default_database_name = 'proto_test_db'` |
| 测试数据库                    | `proto_test_db`（DBCOMPATIBILITY 'B'）    |
| 测试用户                      | `mysql_test_db`（已执行 `set_native_password` 原生密码认证） |
| 客户端接入认证                | pg_hba.conf：`host all mysql_test_db 0.0.0.0/0 sha256` |
| 防火墙                        | firewalld 放行 3308/tcp                   |
| 测试表                        | `` `user` ``（id INT AUTO_INCREMENT PK, name VARCHAR(50), age INT），预置 3 行；t_cmd（协议命令专项，含 LONGBLOB 列，预置 500 行） |

# 3 版本概要测试结论、关键风险和规避措施

## 3.1 测试结论总结

本测试共设计 37 个测试用例：七个维度 34 个（连接与认证 4、类型映射与文档参数 8、DML 批量 3、事务 5、连接池 2、已知限制专项 3、协议命令专项 9）+ Spring Boot 冒烟 3 个。**37 个用例全部通过**（"通过"= 行为与断言一致；差异行为以 FINDING 记录）。

核心结论：

1. Spring Boot 3.x（BOM 管理的 mysql-connector-j 新坐标）配合 openGauss B 模式，基础 CRUD、事务、批量、类型映射、连接池、协议命令均可正常工作；
2. **基础功能版本边界不在驱动**：8 档驱动下基础 CRUD/事务/批量/类型映射/连接池/文档参数行为逐项一致，官方文档"8.0.28 及其以下"的版本范围表述可以上探；但**协议命令专项行为与驱动版本相关（三档）**：8.0.28 档 COM_STMT_RESET 可复现地崩溃服务端进程（FINDING I）、8.0.28~8.4.0 档 SEND_LONG_DATA 1MB 丢末块 8KB（FINDING H）、9.7.0/26.7.0 档 37/37 全部通过（详见 4.2）；
3. 记录 FINDING 7 项（A/B/D/F/G/H/I）：FINDING A 实为默认配置（`dolphin.lower_case_table_names=0`）下的大小写敏感行为，设置 `dolphin.lower_case_table_names=1` 并持久化后实测恢复；FINDING B 实为 `dolphin.sql_mode` 的 `no_zero_date` 配置行为（默认开启拒绝，去掉后零日期可存储、三档 zeroDateTimeBehavior 读取均生效）；FINDING D 为隔离级别映射；FINDING F/G 为协议命令专项新发现，FINDING H/I 为驱动矩阵全量补测新发现（详见 3.3、5.2）；原 FINDING C 经补测改写为正向确认（useOldAliasMetadataBehavior 参数在表列别名场景生效，表达式别名场景与文档备注一致）。

## 3.2 约束说明

1. 本测试覆盖 JDBC 基础使用场景（连接/类型/批量/事务/连接池/协议命令），未覆盖存储过程与真游标逐批拉取行为（见维度六、七）；
2. 测试基于 openGauss 7.0.0-RC3，正式 7.0.0-LTS 发布后建议用同一套用例回归；
3. 批量性能对比（rewriteBatchedStatements）为单机单次测量，量级参考，非基准测试；
4. 本次未实测 caching_sha2_password 认证方式与 TLS/SSL 连接（需服务端 SSL 配置），版本范围结论仅针对 mysql_native_password 认证与 JDBC 驱动，不覆盖 MySQL 命令行客户端。

## 3.3 关键风险和规避措施

| 风险 | 说明 | 规避措施 |
| ---- | ---- | -------- |
| FINDING A：JDBC 元数据 API 不可用 | `getTables` 报错 `information_schema.TABLES does not exist`——默认配置（`dolphin.lower_case_table_names=0`）下大小写敏感：视图本身存在、小写可查，驱动按 MySQL 惯例发大写 TABLES 报不存在，依赖元数据探测的 ORM/工具可能受影响 | **将 `dolphin.lower_case_table_names` 设为 1 并持久化（postgresql.conf 追加后重启）**，实测 `DatabaseMetaData.getTables` 恢复正常；未配置时可使用 B 模式支持的元数据方式（直接查询系统表）或避开该 API |
| FINDING B：零日期 | 零日期（`0000-00-00`）存储由 `dolphin.sql_mode` 的 `no_zero_date` 控制：默认开启，写入被拒（`date/time field value out of range`）；从 sql_mode 中去掉后可正常存储 | 需要存储零日期时去掉 `no_zero_date`；表中有零日期时 `zeroDateTimeBehavior` 三档读取实测均生效（convertToNull 读为 null、exception 抛 `Zero date value prohibited`、round 读为 `0001-01-01`，与 MySQL 语义一致） |
| FINDING C（已改写为正向确认） | `useOldAliasMetadataBehavior` 表列别名场景参数生效（true 时 getColumnName 返回别名），表达式别名场景两种设置一致是文档规定的预期行为 | 无风险 |
| FINDING D：隔离级别映射 | SERIALIZABLE 被服务端映射为 REPEATABLE_READ（读回 4） | 应用按实际生效级别（REPEATABLE_READ）设计并发控制 |
| FINDING F：SEND_LONG_DATA 二进制参数遇 0x00 截断 | 服务端预编译（useServerPrepStmts=true）+ setBlob 流式写入 LONGBLOB 时，二进制内容遇第一个 0x00 字节被服务端按 C 字符串截断（仅存前缀）；无零字节的 1MB 数据完整往返（MD5 一致）；客户端预编译（文本协议）路径无此问题 | 服务端预编译路径避免写入含 0x00 的二进制数据（使用客户端预编译文本协议路径），或修复后回归 |
| FINDING G：多语句结果返回结构不完整 | `allowMultiQueries=true` 多语句执行时，execute() 只反映首语句结果，getMoreResults() 取不到后续语句的结果集（B 组诊断：仅回传首语句结果 + 末 DML update count，中间结果全部丢弃；纯 SELECT 序列则逐个正常返回）；各语句的 DML 均已落库 | 逐条执行或仅依赖末条 update count；服务端修复后回归 |
| FINDING H：SEND_LONG_DATA 1MB 丢末块 8KB（8.0.28~8.4.0 档） | 服务端预编译 + setBlob 写入 1MB 全 0x41（无零字节）数据时，8.0.28/8.0.33/8.1.0/8.3.0/8.4.0 档读回仅 1040384 字节 = 127×8192，恰少末块 8192 字节，MD5 校验失败（两个工程同构复现）；9.7.0/26.7.0 档完整往返。断言行固定 `expected: 1048576 but was: 1040384`，触发条件与驱动分块方式相关（8192 字节对齐边界），缺陷在服务端 | 8.0.28~8.4.0 档服务端预编译路径避免写入 8192 倍数大小的大二进制（改用 9.7.0+ 驱动或客户端预编译文本协议路径）；服务端修复后回归 |
| 存储过程 / 真游标 | 存储过程 prepareCall 报 `PARAMETERS does not exist` 实为默认配置下大小写敏感所致（视图存在、小写可查，`lower_case_table_names=1` 后大写可查）；协议层对游标与存储过程功能支持有限（文档已声明） | 改写为普通 SQL / 批处理；详见维度六、七 |
| FINDING I：COM_STMT_RESET 崩溃服务端进程（仅 8.0.28 档） | 8.0.28 档 clearParameters 后二次 executeUpdate（驱动先发 COM_STMT_RESET）直接崩溃 openGauss 服务端进程：客户端报 `EOFException: read 0 bytes before connection was unexpectedly lost`（响应中途断连），服务端日志在崩溃时刻戛然而止（无 PANIC、无正常关闭流程），3308 端口不可达，需人工重启；连续复现 3 次（单测可复现，两个工程同构）。8.0.33 及以上档同一序列正常，触发条件依赖驱动版本 | 8.0.28 档避免服务端预编译语句 clearParameters 后复用（改为新建语句或换 8.0.33+ 驱动）；服务端修复后回归 |

# 4 版本详细测试结论

## 4.1 特性测试结论

### 4.1.1 连接与认证（4 用例，全部通过）

| 用例 | 结果 |
| ---- | ---- |
| 正常连接（url 指向 3308 协议口） | ✓；productName = `MySQL`，productVersion = `8.0.28-dophin-server`（服务端返回的官方拼写） |
| 错误密码 | ✓ 拒绝：`failed in auth check, role:mysql_test_db` |
| 错误用户名 | ✓ 拒绝：`no pg_hba.conf entry ...` |
| mysql 驱动连 5432 原生口 | ✓ 失败：`Communications link failure`（MySQL 协议只在 3308） |
| FINDING A | `DatabaseMetaData.getTables` 报错 `relation "information_schema.TABLES" does not exist on primary`（默认配置 `dolphin.lower_case_table_names=0` 下大小写敏感：视图存在、小写可查；设置 =1 并持久化后实测 getTables 恢复正常） |

### 4.1.2 类型映射与文档参数（8 用例，全部通过）

| 类型 / 参数 | 结果 |
| ----------- | ---- |
| TINYINT → Integer | ✓ |
| TINYINT(1) → Boolean（tinyInt1isBit 默认 true） | ✓ |
| BIT(1) → Boolean | ✓ 与官方协议兼容文档备注一致（"不管是否转换，在 Java 中都是 Boolean 类型"） |
| 服务端列类型元数据 | ✓ 观测：TINYINT(1) 与 BIT(1) 均上报 `typeName=BIT`（jdbcType=-7）——默认参数下两者均映射 Boolean 的根源 |
| DECIMAL(10,2) → BigDecimal | ✓ |
| DATE → java.sql.Date / DATETIME → LocalDateTime / TIMESTAMP → Timestamp | ✓ |
| VARCHAR → String / BLOB 读写往返（9 字节） | ✓ |
| `tinyInt1isBit=false` → Integer | ✓ 参数生效 |
| `transformedBitIsBoolean=false/true` | ✓ 两种设置 getObject 均映射 Boolean（与官方文档备注一致）；getBytes 恒返回原始字节 `{1}`（与参数无关）。原 1.0 版"false→byte[]"结论系 getBytes 观测口径，已修正 |
| `useOldAliasMetadataBehavior=true/默认` | ✓ **参数生效（原 FINDING C 改写为正向确认）**：表列别名 `SELECT name AS n` 场景，true 时 getColumnName 返回别名 `n`，默认返回原始列名 `name`；表达式别名 `SELECT 1 AS my_alias` 场景两种设置均返回别名，符合文档备注"表达式别名始终返回别名" |
| FINDING B | ✓ 零日期由 `dolphin.sql_mode` 的 `no_zero_date` 控制：默认开启，`0000-00-00 00:00:00` 写入被拒（`date/time field value out of range`）；会话级去掉 no_zero_date 后可正常存储；表中有零日期时 zeroDateTimeBehavior 三档实测均生效——convertToNull 读为 null、exception 读时抛 `Zero date value prohibited`、round 读为 `0001-01-01`（与 MySQL 语义一致） |
| `useaffectedrows=true/默认` | ✓ 与官方文档备注一致（"目前不支持"）：无变化 UPDATE 两种设置均返回 1（找到的行数）；MySQL 原生在 =true 时应返回 0（受影响行数），B 模式恒返回找到行数 |
| `allowMultiQueries=true/默认` | ✓ 参数生效：true 时 `SELECT 1; SELECT 2` 两个结果集均返回；默认时服务端明确拒绝（`Multi-statement is not allowed. Please connect with allowMultiQueries=true`） |

### 4.1.3 DML 批量（3 用例，全部通过）

| 用例 | 结果 |
| ---- | ---- |
| PreparedStatement.addBatch 1000 行 | ✓ executeBatch 返回 1000 条计数 |
| rewriteBatchedStatements=true vs 默认 | ✓ **约 17 倍提速**（500 行 59ms vs 1020ms）→ B 模式协议层真实支持批量 rewrite，建议指导文档推荐开启 |
| JdbcTemplate.batchUpdate | ✓ |

### 4.1.4 事务（5 用例，全部通过）

| 用例 | 结果 |
| ---- | ---- |
| TransactionTemplate 提交后可见 / setRollbackOnly 回滚后不可见 | ✓ |
| 手动 Connection commit / rollback | ✓ |
| SAVEPOINT 保存点 + 部分回滚 | ✓ |
| 隔离级别 READ_COMMITTED / REPEATABLE_READ / SERIALIZABLE | ✓ 均被接受 |
| FINDING D | setTransactionIsolation(SERIALIZABLE) 后读回 4（REPEATABLE_READ）→ 服务端将 SERIALIZABLE 映射/回落为 REPEATABLE_READ |

### 4.1.5 连接池（2 用例，全部通过）

| 用例 | 结果 |
| ---- | ---- |
| Boot 默认 HikariCP，maximumPoolSize=10 生效 | ✓ |
| CONNECTION_ID() 两次取连接 id 相同（池大小=1） | ✓ 连接复用确认；CONNECTION_ID() 为 B 模式支持的 MySQL 函数，可作为文档补充点 |

### 4.1.6 已知限制专项（3 用例）

| 用例 | 结果 |
| ---- | ---- |
| 游标：useCursorFetch=true + useServerPrepStmts=true + setFetchSize(100) | 驱动层无报错、完整读回 2000 行；**协议层是否真逐批返回待确认**（无报错≠真游标，报告按"驱动层可用、协议层待确认"记录） |
| 普通 Statement.setFetchSize(50) | 无报错、完整读回（可能被静默忽略） |
| 存储过程 CallableStatement | ✓ 报错（符合文档"不支持"）：`relation "information_schema.PARAMETERS" does not exist on primary` —— 技术根源：实为默认配置（`dolphin.lower_case_table_names=0`）下大小写敏感所致，PARAMETERS 视图存在、小写 parameters 实测可查，设置 `dolphin.lower_case_table_names=1` 后大写可查；协议层对存储过程功能支持有限 |

### 4.1.7 冒烟（3 用例，全部通过）

基于 `@SpringBootTest` 全链路冒烟（JdbcTemplate / TxService），覆盖示例工程主干业务路径：

| 用例 | 结果 |
| ---- | ---- |
| JdbcTemplate CRUD（insert → select → update → delete） | ✓ |
| JdbcTemplate batchUpdate 批量插入 | ✓ 返回 2 条计数 |
| 事务回滚（TxService 抛异常触发回滚） | ✓ 回滚后表中无残留行 |

### 4.1.8 协议命令专项（9 用例）

> 背景：评审意见要求增加协议命令基础功能测试（COM_STMT_PREPARE / COM_STMT_SEND_LONG_DATA / COM_STMT_EXECUTE，最好覆盖到每个 command）。MySQL 协议命令按"JDBC 是否有触发路径"处理：可触发的逐条实测（用例 7.1-7.9），无触发路径的在下方说明。

| 协议命令 | JDBC 触发方式 | 用例 | 结果 |
| -------- | ------------- | ---- | ---- |
| COM_QUERY | 普通 Statement 执行 SQL（文本协议，建表/插入/查询均走此命令） | 7.1 | ✓ 插入 2 行后计数 +2（文本协议插入/查询/计数正常） |
| COM_INIT_DB | url 指定 database 建立连接（握手携带）；setCatalog + useLocalSessionState=false | 7.2 | ✓ 连接 catalog=测试库；setCatalog 切换后查询正常 |
| COM_PING | Connection.isValid(2) | 7.3 | ✓ isValid=true |
| COM_STMT_PREPARE | useServerPrepStmts=true 下 prepareStatement | 7.4 | ✓ 服务端预编译插入+查询往返正常（cachePrepStmts=false 确保真实发出） |
| COM_STMT_EXECUTE | 服务端预编译语句 executeQuery/executeUpdate | 7.4 | ✓ 同 7.4 |
| COM_STMT_CLOSE | 服务端预编译语句 close | 7.4 | ✓ 同 7.4（语句关闭无异常） |
| COM_STMT_SEND_LONG_DATA | 服务端预编译 + setBlob 流式写入 1MB LONGBLOB（MD5 往返校验） | 7.5 | ✓ 链路可用（9.7.0 基线），但发现 FINDING F：无零字节 1MB（全 0x41）完整往返 MD5 一致；含 0x00 的 200 字节读回仅 150 字节（= 第一个 0x00 前的前缀）——服务端对二进制参数按 C 字符串截断（详见 5.2.2，回归断言）。驱动矩阵补测发现 FINDING H：8.0.28~8.4.0 档无零字节 1MB 读回仅 1040384 字节（丢末块 8KB，详见 4.2/5.2.2） |
| COM_STMT_RESET | clearParameters 后复用同一预编译语句 | 7.6 | ✓（8.0.33 及以上档）clearParameters 后复用同一语句插入 2 行均生效；但 8.0.28 档此序列（COM_STMT_RESET）可复现崩溃服务端进程（FINDING I，详见 5.2.2） |
| COM_STMT_FETCH | useCursorFetch=true + setFetchSize(100) | 7.7 | ✓ 驱动层无报错、完整读回 500 行；协议层是否真逐批返回需服务端日志佐证（口径同 6.1，按"驱动层可用、协议层待确认"记录） |
| COM_SET_OPTION | allowMultiQueries=true 多语句执行 | 7.8 | ✓ 参数生效、多语句 DML 全部落库，但发现 FINDING G：execute() 仅返回首语句 update count，getMoreResults() 取不到后续 SELECT 结果集（后续 SELECT 被丢弃；纯 SELECT 序列逐个正常）（详见 5.2.2，回归断言） |
| COM_QUIT | Connection.close | 7.9 | ✓ close 后 isClosed=true |

JDBC 无触发路径的命令：COM_FIELD_LIST（已被查询结果集元数据取代）、COM_PROCESS_INFO / COM_STATISTICS / COM_PROCESS_KILL（管理类命令）、COM_CHANGE_USER、COM_REFRESH 等，MySQL JDBC 驱动不提供对应 API，无法从应用层触发，不纳入用例；本专项已覆盖**驱动可触发的全部协议命令**。

## 4.2 兼容性测试结论（驱动版本矩阵）

同一套 37 用例扫描 8 档驱动（8 个 Maven profile 切换，同一服务端环境；其中 BOM 默认档与显式 9.7.0 为同一版本的两条管理路径）：

| 驱动版本 | 坐标 | 结果 |
| -------- | ---- | ---- |
| 8.0.28（文档基准） | mysql:mysql-connector-java（旧坐标） | 协议命令类 1 失败 + 6 错误：SEND_LONG_DATA 丢末块 8KB（FINDING H）、COM_STMT_RESET 崩溃服务端进程（FINDING I，后续用例全部失联，需人工重启服务端）；其余 28 用例全部通过 |
| 8.0.33 | com.mysql:mysql-connector-j（新坐标） | 37 用例中 1 失败（FINDING H：SEND_LONG_DATA 丢末块 8KB），其余 36 通过（COM_STMT_RESET 正常、服务端未崩溃） |
| 8.1.0 | 同上 | 同上（1 失败 + 36 通过） |
| 8.3.0 | 同上 | 同上（1 失败 + 36 通过） |
| 8.4.0 | 同上 | 同上（1 失败 + 36 通过） |
| 9.7.0（Boot 3.5.16 BOM 默认档） | com.mysql:mysql-connector-j（版本由 BOM 管理） | 37/37 通过 |
| 9.7.0（显式 pin） | 同上 | 与 BOM 默认档同一版本同一 jar（仅 Maven 管理路径不同），行为同 BOM 默认档实测 37/37 |
| 26.7.0（最新） | 同上 | 37/37 通过 |

8 档驱动的类型映射、批量提速、事务隔离映射、元数据报错（大小写敏感）、零日期行为（默认拒绝/去掉 no_zero_date 可存储/三档读取生效）、游标行为、存储过程报错、6 个文档参数行为、多语句结果结构（FINDING G）**逐项一致**；协议命令行为按驱动版本分**三档**：8.0.28 崩溃 / 8.0.33~8.4.0 丢 8KB / 9.7.0 及以上正常（FINDING H/I，详见 5.2.2）。

**结论：基础功能（CRUD/事务/批量/类型映射/连接池/文档参数）与驱动版本无关，8 档均可正常工作；协议命令专项存在两个与驱动版本相关的服务端缺陷（FINDING H/I）。** 建议官方文档的版本范围表述由"MySQL JDBC driver：8.0.28 及其以下"更新为"MySQL JDBC 驱动 8.0.28 及以上版本在基础 CRUD/事务/批量/类型映射场景均可正常工作；服务端预编译 + SEND_LONG_DATA/RESET 场景建议使用 9.7.0 及以上驱动"（对应 docs 仓 `dolphin_mysql_protocol_compatibility` 文档 PR，仅针对 JDBC 驱动表述；MySQL 命令行客户端未实测）。

# 5 测试对象质量评估

## 5.1 覆盖率分析

| 覆盖维度 | 覆盖情况 |
| -------- | -------- |
| 功能覆盖 | 连接/认证/类型映射/DML 批量/事务/连接池/已知限制/协议命令七维度 34 用例 + 冒烟 3 用例；官方协议兼容文档列出的 6 个 JDBC 参数与 JDBC 可触发的全部协议命令均已覆盖 |
| 版本覆盖 | 驱动 8 档（BOM 默认档 + 7 个显式版本，8.0.28 ~ 26.7.0，覆盖新旧两套 Maven 坐标） |
| 未覆盖项 | 存储过程（协议不支持，仅验证报错行为）、真游标协议层逐批返回行为（7.7 已覆盖驱动层路径，服务端行为待抓包确认）、caching_sha2_password 认证、TLS/SSL 连接、MySQL 命令行客户端、高并发场景、故障注入、JDBC 无触发路径的协议命令（COM_FIELD_LIST/COM_PROCESS_INFO/COM_STATISTICS/COM_PROCESS_KILL/COM_CHANGE_USER 等，见 4.1.8） |

## 5.2 缺陷统计和分析

### 5.2.1 缺陷统计

| 类别 | 数量 | 说明 |
| ---- | ---- | ---- |
| 服务端差异（FINDING） | 5 | A/B/D/F/G：A/D 为行为差异或元数据缺失，B 为配置行为（no_zero_date 可去），F/G 为协议命令专项新发现（二进制参数 0x00 截断 / 多语句结果结构不完整）；均非崩溃类缺陷（原 FINDING C 经补测改写为正向确认，见 4.1.2） |
| 崩溃类缺陷 | 0 | 无 |

### 5.2.2 缺陷列表

| 编号 | 现象 | 定位 | 严重程度 | 规避措施 |
| ---- | ---- | ---- | -------- | -------- |
| FINDING A | getTables 报错 information_schema.TABLES 不存在 | 默认配置（`dolphin.lower_case_table_names=0`）下大小写敏感：视图存在、小写 tables 实测可查，驱动按 MySQL 惯例发大写 TABLES 报不存在 | 中（影响依赖元数据探测的工具/ORM） | **`dolphin.lower_case_table_names=1` 并持久化（实测 getTables 恢复正常）**；未配置时避开该 API |
| FINDING B | 零日期写入被拒 | `dolphin.sql_mode` 的 `no_zero_date` 默认开启（写入报 `date/time field value out of range`）；去掉后可存储，此时 zeroDateTimeBehavior 三档读取均生效 | 低（默认拒绝符合数据库常规语义，且可按需放开） | 需要存储零日期时去掉 no_zero_date；否则应用层避免零日期 |
| FINDING D | SERIALIZABLE 读回 REPEATABLE_READ | 服务端隔离级别映射 | 中（并发控制设计需注意） | 按 REPEATABLE_READ 设计 |
| FINDING F | COM_STMT_SEND_LONG_DATA 写入含 0x00 的二进制被截断 | 服务端预编译路径对二进制参数按 C 字符串处理：遇第一个 0x00 截断为前缀（200 字节读回 150 = 0x00 位置）；无零字节 1MB 完整往返（MD5 一致）；客户端预编译（文本协议）路径无此问题 | 中（影响服务端预编译 + 二进制大对象写入的正确性） | 服务端预编译路径避免写入含 0x00 的二进制（用客户端预编译文本协议路径） |
| FINDING G | 多语句后续 SELECT 结果集经 getMoreResults() 不可得 | `allowMultiQueries=true` 下结果返回结构不完整：仅回传首语句结果 + 末 DML update count，中间结果全部丢弃（纯 SELECT 序列逐个正常）；各语句 DML 均已落库 | 中（影响依赖多结果集解析的应用/框架） | 逐条执行或仅依赖末条 update count |
| FINDING H | SEND_LONG_DATA 1MB 丢末块 8KB（8.0.28~8.4.0 档） | 服务端预编译 + setBlob 写入 1MB 全 0x41（无零字节）数据时，8.0.28~8.4.0 档读回仅 1040384 字节 = 127×8192（恰少末块 8192），MD5 失败（断言行固定 `expected: 1048576 but was: 1040384`）；9.7.0/26.7.0 档完整往返。与驱动分块方式（8192 对齐）相关，缺陷在服务端 | 高（服务端预编译 + 大二进制写入数据静默丢失） | 8.0.28~8.4.0 档避免服务端预编译写入 8192 倍数大小的大二进制（改用 9.7.0+ 驱动或客户端预编译文本协议路径） |
| FINDING I | COM_STMT_RESET 崩溃服务端进程（仅 8.0.28 档） | 8.0.28 档 clearParameters 后二次 executeUpdate（驱动先发 COM_STMT_RESET）直接崩溃 openGauss 服务端：客户端报 `EOFException: read 0 bytes before connection was unexpectedly lost`，服务端日志戛然而止（无 PANIC、无正常关闭流程），3308 不可达需人工重启；单测可复现 3 次（两个工程同构）；8.0.33+ 档同一序列正常 | 严重（单次 JDBC 操作即可打崩整个服务端，可被任意客户端触发拒绝服务） | 8.0.28 档避免服务端预编译语句 clearParameters 后复用（新建语句或换 8.0.33+ 驱动）；服务端修复后回归 |
| FINDING（存储过程） | prepareCall 报错 PARAMETERS 不存在 | 默认配置下大小写敏感（小写 parameters 实测存在，`lower_case_table_names=1` 后大写可查；存储过程本身文档已声明不支持） | 低（文档已声明） | 改写为普通 SQL |

# 6 测试过程评估

## 6.1 测试策略回顾

采用"控制变量 + 矩阵扫描"策略：先以 Boot BOM 默认驱动（9.7.0）跑全七维度建立基线，再以 Maven profile 切换驱动版本扫描矩阵；对每一项与 MySQL 官方文档行为的差异，用最小复现定位差异归属（驱动侧 or 服务端侧）。

## 6.2 测试设计评估

1. 逐项覆盖了官方协议兼容文档（dolphin_mysql_protocol_compatibility）列出的全部 6 个 JDBC 参数（zeroDateTimeBehavior / tinyInt1isBit / transformedBitIsBoolean / useOldAliasMetadataBehavior / useaffectedrows / allowMultiQueries）：3 项验证生效（tinyInt1isBit、useOldAliasMetadataBehavior、allowMultiQueries），3 项行为与文档声明一致（transformedBitIsBoolean：两种设置 Java 侧均映射 Boolean；useaffectedrows：恒返回找到的行数；zeroDateTimeBehavior：去掉 `no_zero_date` 存储零日期后三档读取均生效）；
2. 覆盖了文档明示的已知限制（游标、存储过程），并定位了存储过程报错的技术根源（默认配置下 information_schema 表名大小写敏感，`dolphin.lower_case_table_names=1` 持久化后大写可查）；
3. 批量用例同时验证了功能与性能（rewriteBatchedStatements 17 倍提速），为文档推荐参数提供了数据支撑；
4. 协议命令专项把评审要求的"覆盖到每个 command"落成 9 个用例：按 JDBC 触发路径逐条实测 COM_QUERY/INIT_DB/PING/STMT_PREPARE/STMT_EXECUTE/STMT_CLOSE/STMT_SEND_LONG_DATA/STMT_RESET/STMT_FETCH/SET_OPTION/QUIT，无 JDBC 触发路径的命令在 4.1.8 说明原因。

## 6.3 测试执行评估

### 6.3.1 测试执行统计数据

| 项目 | 数据 |
| ---- | ---- |
| 用例总数 | 37 |
| 驱动档位 | 8 档（BOM 默认档 + 7 个显式版本；含文档基准 8.0.28 与最新 26.7.0） |
| 执行轮次 | 8 档各一轮全量回归（8.0.28 档因 FINDING I 服务端崩溃分两段执行：协议类 9 + 其余 28）+ 若干轮开发调试与单用例复测 |
| 总执行用例次 | 37 × 8 = 296（含 8.0.28 档两段） |

### 6.3.2 测试用例执行结果统计数据

| 结果 | 数量 |
| ---- | ---- |
| 通过 | 285（8.0.28 档 30 / 8.0.33~8.4.0 档各 36 / 9.7.0 两档 37+37 / 26.7.0 档 37） |
| 失败 | 5（FINDING H 断言失败：8.0.28 档 1 + 8.0.33~8.4.0 档各 1） |
| 错误 | 6（FINDING I 服务端崩溃后失联：8.0.28 档 6） |

# 7 附件

## 7.1 附件1：相关产物

| 产物 | 位置 |
| ---- | ---- |
| 测试代码（37 用例：七维度 34 + 冒烟 3） | 本目录 `src/test/java/org/opengauss/`（cases/ 与 smoke/） |
| 测试思维导图 | 本目录 `docs/测试思维导图-SpringBoot连接openGaussB库.{mm,md,html}` |
| 指导文档 | docs 仓 `docs/zh/mysql_compatibility_developer_guide/springboot_development.md`（轻量版 `docs-lite/zh/...` 同路径） |
