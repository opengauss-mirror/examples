# 基于SpringDataJPA框架的openGauss数据库测试

版权所有 © 2026  openGauss社区
 您对“本文档”的复制、使用、修改及分发受知识共享(Creative Commons)署名—相同方式共享4.0国际公共许可协议(以下简称“CC BY-SA 4.0”)的约束。为了方便用户理解，您可以通过访问[*https://creativecommons.org/licenses/by-sa/4.0/*](https://creativecommons.org/licenses/by-sa/4.0/) 了解CC BY-SA 4.0的概要 (但不是替代)。CC BY-SA 4.0的完整协议内容您可以访问如下网址获取：[*https://creativecommons.org/licenses/by-sa/4.0/legalcode*](https://creativecommons.org/licenses/by-sa/4.0/legalcode)。

修订记录

| 日期      | 修订版本 | 修改描述                                          | 作者 |
| --------- | -------- | ------------------------------------------------- | ---- |
| 2026-08-28 | 1.0     | openGauss 对 Spring Data JPA + MySQL JDBC 的兼容性测试 | lele-2026 |
| 2026-09-04 | 1.1     | 协议命令专项 9 用例实测结果回填（新增 FINDING F/G）；LIKE escape 转义语义差异表述与 docs 仓指导文档对齐（escape '\\' 两个反斜杠、与 Plugin 仓 issue #504 同源）；FINDING E 定位修正（默认配置 `dolphin.lower_case_table_names=0` 下大小写敏感，=1 持久化可恢复） | lele-2026 |
| 2026-09-04 | 1.2     | 驱动矩阵全量补测：协议命令行为按驱动版本分三档（新增 FINDING H/I，与 #27 报告同源）——SEND_LONG_DATA 1MB 丢末块 8KB（8.0.28~8.4.0 档）、COM_STMT_RESET 崩溃服务端进程（仅 8.0.28 档，可复现）；9.7.0/26.7.0 档 19/19 全绿；矩阵结论由"行为逐项一致"改写为三档行为 | lele-2026 |
| 2026-09-04 | 1.3     | 转义字符问题不再以 Bug 描述：全文统一为"转义语义差异（已知差异，与 Plugin 仓 issue #504 同源）"，与 docs 仓指导文档口径一致（涉及摘要、3.1/3.3/4.1.1/4.2/5.1/5.2/6.1/6.2/7.1/7.2/7.3、思维导图、README 与测试代码注释） | lele-2026 |
| 2026-09-04 | 1.4     | 删除 7.1 遗留问题列表（内部跟踪事项不再在公开报告列出，各差异/FINDING 的现状与规避已完整记录于 3.3/5.2；与 #27 报告同步处理）；附件 7.2/7.3 重编号为 7.1/7.2；环境表移除内网 IP；附件表移除"原始日志备存"行 | lele-2026 |

[TOC]

**关键词**：

Spring Data JPA，Hibernate，MySQL JDBC，openGauss 数据库，B 兼容模式，dolphin 插件，兼容性测试

**Abstract 摘要**：

本测试验证使用 MySQL JDBC 驱动（mysql-connector-j）连接 openGauss B 兼容模式数据库，在 Spring Data JPA（Hibernate 6.6）框架下的兼容性。测试覆盖已知差异回归、ddl-auto 行为、类型映射、分页与批量、冒烟、协议命令六个方面共 19 个用例，并扫描 7 个驱动版本验证版本边界。结果显示：Spring Data JPA 在 openGauss B 模式下类型映射、主键回填、分页、批量、事务等核心链路均可正常工作；发现并定位 6 个问题——LIKE 模糊查询 escape 转义语义差异（已有 workaround，与社区已记录的已知差异同源，参见 Plugin 仓 issue #504）、FINDING E（ddl-auto=update 静默失效，根因为默认配置 `dolphin.lower_case_table_names=0` 下 information_schema 表名大小写敏感（Hibernate 发大写 KEYWORDS 报不存在，小写 keywords 实测可查），设置 =1 并持久化后实测恢复）、FINDING F（COM_STMT_SEND_LONG_DATA 二进制参数遇 0x00 截断）、FINDING G（多语句结果返回结构不完整）、FINDING H（SEND_LONG_DATA 1MB 丢末块 8KB，8.0.28~8.4.0 档）与 FINDING I（COM_STMT_RESET 崩溃服务端进程，仅 8.0.28 档）。该转义差异与 FINDING E/F/G 均与驱动版本无关，根源在服务端；FINDING H/I 为服务端缺陷、触发条件依赖驱动版本。

**缩略语清单：**

| 缩略语 | 英文全名                    | 中文解释       |
| ------ | --------------------------- | -------------- |
| JPA    | Jakarta Persistence API     | Java 持久化 API |
| ORM    | Object Relational Mapping   | 对象关系映射   |
| JPQL   | Jakarta Persistence Query Language | JPA 查询语言 |
| ddl-auto | Hibernate DDL 自动管理配置 | 建表策略配置   |

***

# 1 概述

本报告针对 Spring Data JPA 框架（Hibernate 6 实现）配合 MySQL JDBC 驱动与 openGauss B 兼容模式数据库的兼容性测试活动进行总结。Spring Data JPA 是 Spring Boot 生态最常用的 ORM 方案，其兼容性风险集中在方言适配、元数据探测、主键生成与查询渲染四个环节，本测试针对性地覆盖了这些风险点。

# 2 测试版本说明

## 2.1 测试版本信息

### 2.1.1 被测版本

| 版本名称   | 软件包名称                    | 测试起始时间 | 测试结束时间 | 测试人员 |
| ---------- | ----------------------------- | ------------ | ------------ | -------- |
| Spring Boot | spring-boot-starter-data-jpa 3.5.16 | 2026-08-28 | 2026-08-28 | lele-2026 |
| Hibernate  | hibernate-core 6.6.53（Boot BOM 管理） | 2026-08-28 | 2026-08-28 | lele-2026 |
| mysql-connector-j | 9.7.0（Boot BOM 默认） | 2026-08-28 | 2026-08-28 | lele-2026 |
| openGauss  | openGauss 7.0.0-RC3（源码编译，commit fe6f6e4b） | 2026-08-28 | 2026-08-28 | lele-2026 |

> 说明：任务要求基于 openGauss 7.0.0-LTS，实测环境为 7.0.0-RC3（与 issue 开发指导文档同版本），正式版发布后建议回归。
>
> 说明：被测二进制基于 7.0.0-RC3（commit fe6f6e4b）的本地源码工作副本编译：dolphin 插件为本地 contrib/dolphin 副本，包含此前 round() 支持 blob 任务的本地改动（已核验二进制中存在 round(binary/blob) 系列函数签名），contrib/timescaledb 未参与编译。上述差异均不在本测试覆盖的 MySQL 协议 / JDBC / 类型映射 / 事务路径上，不影响测试结论。

### 2.1.2 配套测试的版本

| 版本名称 | 配套版本        | 版本说明                                        |
| -------- | --------------- | ----------------------------------------------- |
| JDK      | 17.0.15（Liberica） | Spring Boot 3.x 要求 JDK 17+                |
| Maven    | 3.9.14          | 构建工具                                        |
| 驱动矩阵 | 7 个版本        | 8.0.28（旧坐标）/ 8.0.33 / 8.1.0 / 8.3.0 / 8.4.0 / 9.7.0 / 26.7.0 |

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
| 测试表                        | `` `user` ``（预置 3 行）、t_jpa_map（类型映射）、t_jpa_ddl（ddl-auto 专项）、jpa_cmd（协议命令专项，含 LONGBLOB 列，预置 500 行） |

# 3 版本概要测试结论、关键风险和规避措施

## 3.1 测试结论总结

本测试共设计 19 个测试用例，覆盖六个方面：已知差异回归（2）、ddl-auto 专项（2）、类型映射（1）、分页与批量（2）、冒烟（3）、协议命令专项（9）。**19 个用例在 BOM 默认档（9.7.0）全部通过**（已知差异用例的"通过"= 复现了预期中的当前差异行为）。

核心结论：

1. Spring Data JPA 在 openGauss B 模式下的核心链路——类型映射、IDENTITY 主键回填、分页、批量保存、事务回滚——全部可用；
2. **LIKE 模糊查询 escape 转义语义差异**：Hibernate 6.6 默认在所有 LIKE 谓词后追加 `escape '\\'`（两个反斜杠），MySQL 中 `'\\'` 是 1 个字符，而 B 模式按标准 SQL 把 `'\\'` 视为 2 个字符，导致 `invalid escape string`。规避：JPQL 显式 `escape '!'`。该差异的根源是 B 模式不支持反斜杠作为字符串转义字符（与社区已记录的已知差异同源，参见 Plugin 仓 issue #504），与驱动版本无关；
3. **FINDING E（ddl-auto 静默失效）**：Hibernate 启动时查询 `information_schema.KEYWORDS` 失败（仅警告）→ ddl-auto=update 不做任何建表动作且不报错，运行时才暴露 `relation does not exist`。根因为默认配置（`dolphin.lower_case_table_names=0`）下大小写敏感（视图存在、小写可查）。**设置 `dolphin.lower_case_table_names=1` 并持久化后实测大写 KEYWORDS 可查、Hibernate 元数据探测恢复**；未配置时建议 B 模式手工建表 + `ddl-auto=none`；
4. **FINDING F / G（协议命令专项）**：COM_STMT_SEND_LONG_DATA 写入含 0x00 的二进制被按 C 字符串截断为前缀；allowMultiQueries 多语句后续 SELECT 结果集经 getMoreResults() 不可得（均详见 5.2.2，回归断言）；
5. **协议命令行为按驱动版本分三档**：8.0.28 档 COM_STMT_RESET 可复现崩溃服务端进程（FINDING I）、8.0.28~8.4.0 档 SEND_LONG_DATA 1MB 丢末块 8KB（FINDING H）、9.7.0/26.7.0 档 19/19 全部通过；该转义差异与 FINDING E/F/G 在 7 档下行为一致、与驱动版本无关（详见 4.2）。

## 3.2 约束说明

1. ddl-auto=create 模式未测（会 drop+create 全部映射表，破坏现有数据）；其行为预计与 update 一致（同样依赖元数据探测）；
2. Hibernate 批量参数（hibernate.jdbc.batch_size / order_inserts）未专项计时，saveAll 仅验证功能正确性；
3. 测试基于 openGauss 7.0.0-RC3，正式 7.0.0-LTS 发布后建议用同一套用例回归。

## 3.3 关键风险和规避措施

| 风险 | 说明 | 规避措施 |
| ---- | ---- | -------- |
| 转义语义差异：LIKE 模糊查询报错 | 派生查询 / 隐式 escape 渲染 `escape '\\'`（两个反斜杠）→ `invalid escape string` | JPQL 显式 `escape '!'`（本工程 searchByName 为参考实现）；根源是 B 模式不支持反斜杠作为字符串转义字符（与社区已记录的已知差异同源，参见 Plugin 仓 issue #504）；若服务端调整反斜杠转义语义（B 模式字符串字面量支持反斜杠转义）该差异可消除 |
| FINDING E：ddl-auto=update 静默失效 | 容器启动成功但不建表，运行时才报错，排查困难 | **设置 `dolphin.lower_case_table_names=1` 并持久化后实测 Hibernate 元数据探测恢复**；未配置时 B 模式手工建表 + `ddl-auto=none`（或 validate） |
| FINDING A 联动：元数据探测受限 | getTables / prepareCall 依赖 information_schema.TABLES/PARAMETERS | 同 #27 报告 FINDING A：默认配置（`dolphin.lower_case_table_names=0`）下大小写敏感，设置 =1 并持久化后实测 `DatabaseMetaData.getTables` 恢复正常；ORM 场景尽量避免依赖元数据探测的功能 |
| FINDING F：SEND_LONG_DATA 二进制参数遇 0x00 截断 | 服务端预编译 + setBlob 流式写入 LONGBLOB 时，二进制遇第一个 0x00 字节被按 C 字符串截断为前缀（无零字节 1MB 完整往返 MD5 一致；客户端预编译文本协议路径无此问题） | 服务端预编译路径避免写入含 0x00 的二进制（用客户端预编译文本协议路径）；服务端修复后回归 |
| FINDING G：多语句结果返回结构不完整 | allowMultiQueries=true 多语句执行时 getMoreResults() 取不到后续 SELECT 结果集（仅首语句结果 + 末 DML update count；纯 SELECT 序列逐个正常；DML 均落库） | 逐条执行或仅依赖末条 update count；服务端修复后回归 |
| FINDING H：SEND_LONG_DATA 1MB 丢末块 8KB（8.0.28~8.4.0 档，与 #27 报告同源） | 服务端预编译 + setBlob 写入 1MB 全 0x41（无零字节）数据时，8.0.28/8.0.33/8.1.0/8.3.0/8.4.0 档读回仅 1040384 字节 = 127×8192（恰少末块 8192），MD5 校验失败；9.7.0/26.7.0 档完整往返 | 8.0.28~8.4.0 档服务端预编译路径避免写入 8192 倍数大小的大二进制（改用 9.7.0+ 驱动或客户端预编译文本协议路径）；服务端修复后回归 |
| FINDING I：COM_STMT_RESET 崩溃服务端进程（仅 8.0.28 档，与 #27 报告同源） | 8.0.28 档 clearParameters 后二次 executeUpdate（驱动先发 COM_STMT_RESET）直接崩溃 openGauss 服务端进程：客户端报 `EOFException: read 0 bytes before connection was unexpectedly lost`，3308 端口不可达需人工重启；单测可复现（两个工程同构）；8.0.33 及以上档同一序列正常 | 8.0.28 档避免服务端预编译语句 clearParameters 后复用（新建语句或换 8.0.33+ 驱动）；服务端修复后回归 |

# 4 版本详细测试结论

## 4.1 特性测试结论

### 4.1.1 已知差异回归（2 用例，符合预期）

| 用例 | 结果 |
| ---- | ---- |
| K1：派生 LIKE（findByNameLike） | ✓ 复现转义语义差异：`invalid escape string`（Hibernate 6.6 默认追加 `escape '\\'`（两个反斜杠），MySQL 中 `'\\'`=1 字符，B 模式按标准 SQL 视 `'\\'` 为 2 字符；根源是 B 模式不支持反斜杠作为字符串转义字符，与 Plugin 仓 issue #504 同源）。**本用例为回归断言**：服务端行为调整后测试将自动失败，提醒移除 workaround 并更新文档 |
| K2：JPQL 显式 `escape '!'`（searchByName） | ✓ workaround 稳定可用（分页版模糊查询） |

### 4.1.2 ddl-auto 专项（2 用例）

| 用例 | 结果 |
| ---- | ---- |
| D1：ddl-auto=update | **FINDING E**：容器启动成功，但缺失表 t_jpa_ddl 未被创建，不报错不警告（静默失效）。日志证据：`HHH000339: Could not obtain connection metadata ... relation "information_schema.KEYWORDS" does not exist on primary`（默认配置 `dolphin.lower_case_table_names=0` 下大小写敏感：视图存在、小写 keywords 实测可查）→ 元数据获取失败 → 无法做"现有 schema vs 实体"对比 → update 什么都不做。设置 `dolphin.lower_case_table_names=1` 并持久化后实测大写 KEYWORDS 可查、元数据探测恢复 |
| D2：ddl-auto=validate | 启动校验未报错（但元数据同样缺失，校验深度存疑，如实表述"未报错"） |

### 4.1.3 类型映射（1 用例，通过）

| 用例 | 结果 |
| ---- | ---- |
| M1：五类型往返 + 主键回填 | ✓ BigDecimal / LocalDate / LocalDateTime / Boolean↔TINYINT(1) / String 写入读出一致；save 后 IDENTITY 主键回填正常（getGeneratedKeys 链路可用） |

### 4.1.4 分页与批量（2 用例，通过）

| 用例 | 结果 |
| ---- | ---- |
| P1：PageRequest 多页遍历 | ✓ LIMIT/OFFSET 与 count 查询正常 |
| P2：saveAll(50) | ✓ 批量保存 50 条主键逐个回填 |

### 4.1.5 冒烟（3 用例，通过）

| 用例 | 结果 |
| ---- | ---- |
| saveAndFind / pagination / transactionRollback | ✓ 全链路正常；@Transactional 异常回滚正常 |

### 4.1.6 协议命令专项（9 用例）

> 背景：评审意见要求增加协议命令基础功能测试（COM_STMT_PREPARE / COM_STMT_SEND_LONG_DATA / COM_STMT_EXECUTE，最好覆盖到每个 command）。MySQL 协议命令按"JDBC 是否有触发路径"处理：可触发的逐条实测（用例 C1-C9），无触发路径的在下方说明。与 #27 报告（springboot）7.1-7.9 同一套设计。

| 协议命令 | JDBC 触发方式 | 用例 | 结果 |
| -------- | ------------- | ---- | ---- |
| COM_QUERY | 普通 Statement 执行 SQL（文本协议，建表/插入/查询均走此命令） | C1 | ✓ 插入 2 行后计数 +2（文本协议插入/查询/计数正常） |
| COM_INIT_DB | url 指定 database 建立连接（握手携带）；setCatalog + useLocalSessionState=false | C2 | ✓ 连接 catalog=测试库；setCatalog 切换后查询正常 |
| COM_PING | Connection.isValid(2) | C3 | ✓ isValid=true |
| COM_STMT_PREPARE | useServerPrepStmts=true 下 prepareStatement | C4 | ✓ 服务端预编译插入+查询往返正常（cachePrepStmts=false 确保真实发出） |
| COM_STMT_EXECUTE | 服务端预编译语句 executeQuery/executeUpdate | C4 | ✓ 同 C4 |
| COM_STMT_CLOSE | 服务端预编译语句 close | C4 | ✓ 同 C4（语句关闭无异常） |
| COM_STMT_SEND_LONG_DATA | 服务端预编译 + setBlob 流式写入 1MB LONGBLOB（MD5 往返校验） | C5 | ✓ 链路可用（9.7.0 基线），但发现 FINDING F：无零字节 1MB（全 0x41）完整往返 MD5 一致；含 0x00 的 200 字节读回仅 150 字节（= 第一个 0x00 前的前缀）——服务端对二进制参数按 C 字符串截断（与 #27 报告 FINDING F 同源，详见 5.2.2，回归断言）。驱动矩阵补测发现 FINDING H：8.0.28~8.4.0 档无零字节 1MB 读回仅 1040384 字节（丢末块 8KB，详见 4.2/5.2.2） |
| COM_STMT_RESET | clearParameters 后复用同一预编译语句 | C6 | ✓（8.0.33 及以上档）clearParameters 后复用同一语句插入 2 行均生效；但 8.0.28 档此序列（COM_STMT_RESET）可复现崩溃服务端进程（FINDING I，详见 5.2.2） |
| COM_STMT_FETCH | useCursorFetch=true + setFetchSize(100) | C7 | ✓ 驱动层无报错、完整读回 500 行；协议层是否真逐批返回需服务端日志佐证（口径同 #27 报告 6.1，按"驱动层可用、协议层待确认"记录） |
| COM_SET_OPTION | allowMultiQueries=true 多语句执行 | C8 | ✓ 参数生效、多语句 DML 全部落库，但发现 FINDING G：execute() 仅返回首语句 update count，getMoreResults() 取不到后续 SELECT 结果集（后续 SELECT 被丢弃；纯 SELECT 序列逐个正常）（与 #27 报告 FINDING G 同源，详见 5.2.2，回归断言） |
| COM_QUIT | Connection.close | C9 | ✓ close 后 isClosed=true |

JDBC 无触发路径的命令：COM_FIELD_LIST（已被查询结果集元数据取代）、COM_PROCESS_INFO / COM_STATISTICS / COM_PROCESS_KILL（管理类命令）、COM_CHANGE_USER、COM_REFRESH 等，MySQL JDBC 驱动不提供对应 API，无法从应用层触发，不纳入用例；本专项已覆盖**驱动可触发的全部协议命令**。

## 4.2 兼容性测试结论（驱动版本矩阵）

同一套 19 用例扫描 7 档驱动（Maven profile 切换，同一服务端环境）：

| 驱动版本 | 坐标 | 结果 |
| -------- | ---- | ---- |
| 8.0.28（文档基准） | mysql:mysql-connector-java（旧坐标） | 协议命令类 1 失败 + 6 错误：SEND_LONG_DATA 丢末块 8KB（FINDING H）、COM_STMT_RESET 崩溃服务端进程（FINDING I，后续用例全部失联，需人工重启服务端）；其余 12 用例通过（含 K1/D1 复现） |
| 8.0.33 | com.mysql:mysql-connector-j（新坐标） | 19 用例中 1 失败（FINDING H：SEND_LONG_DATA 丢末块 8KB），其余 18 通过（COM_STMT_RESET 正常、服务端未崩溃） |
| 8.1.0 | 同上 | 同上（1 失败 + 18 通过） |
| 8.3.0 | 同上 | 同上（1 失败 + 18 通过） |
| 8.4.0 | 同上 | 同上（1 失败 + 18 通过） |
| 9.7.0（Boot 3.5.16 BOM 默认） | 同上 | 19/19 通过 |
| 26.7.0（最新） | 同上 | 19/19 通过 |

7 档驱动的核心链路（类型映射/主键回填/分页/批量/事务）与 K1 复现转义语义差异、D1 复现 FINDING E、多语句结果结构（FINDING G）**逐项一致**——这些问题均与驱动版本无关，根源在服务端；协议命令专项按驱动版本分**三档**：8.0.28 崩溃 / 8.0.33~8.4.0 丢 8KB / 9.7.0 及以上正常（FINDING H/I，详见 5.2.2）。与 #27 报告 8 档矩阵结论互相印证。

# 5 测试对象质量评估

## 5.1 覆盖率分析

| 覆盖维度 | 覆盖情况 |
| -------- | -------- |
| 功能覆盖 | 已知差异回归 / ddl-auto / 类型映射+主键回填 / 分页 / 批量 / 事务回滚 / 协议命令，共 19 用例 |
| 风险点覆盖 | ORM 四大风险环节全覆盖：方言（MySQLDialect+自定义适配点）、元数据探测（D1 捕获 HHH000339）、主键生成（IDENTITY 回填）、查询渲染（K1 捕获 escape 渲染差异） |
| 版本覆盖 | 驱动 7 档（8.0.28 ~ 26.7.0，覆盖新旧两套 Maven 坐标） |
| 未覆盖项 | ddl-auto=create（破坏性）、Hibernate 批量参数性能、二级缓存、锁模式、JDBC 无触发路径的协议命令（见 4.1.6） |

## 5.2 缺陷统计和分析

### 5.2.1 缺陷统计

| 类别 | 数量 | 说明 |
| ---- | ---- | ---- |
| 服务端差异（FINDING/已知差异） | 6 | LIKE escape 转义语义差异（与 issue #504 同源）+ FINDING E（ddl-auto 静默失效）+ FINDING F（SEND_LONG_DATA 0x00 截断）+ FINDING G（多语句结果结构不完整）+ FINDING H（SEND_LONG_DATA 1MB 丢末块 8KB，8.0.28~8.4.0 档）+ FINDING I（COM_STMT_RESET 崩溃，仅 8.0.28 档） |
| 崩溃类缺陷 | 1 | FINDING I（8.0.28 档 COM_STMT_RESET 可复现崩溃 openGauss 服务端进程，与 #27 报告同源） |

### 5.2.2 缺陷列表

| 编号 | 现象 | 定位 | 严重程度 | 规避措施 |
| ---- | ---- | ---- | -------- | -------- |
| 转义语义差异（#504） | 派生 LIKE 查询报 `invalid escape string` | Hibernate 6.6 默认渲染 `escape '\\'`（两个反斜杠）；MySQL 中 `'\\'`=1 字符，B 模式按标准 SQL 把 `'\\'` 当 2 字符。根源是 B 模式不支持反斜杠作为字符串转义字符（与社区已记录的已知差异同源，参见 Plugin 仓 issue #504）。与驱动版本无关（8.0.28/9.7.0 同错） | 高（模糊查询是高频功能） | JPQL 显式 `escape '!'`；服务端候选调整：B 模式字符串字面量支持反斜杠转义（已 gsql 取证，见 7.2 附件） |
| FINDING E | ddl-auto=update 容器启动成功但表未创建，静默失效 | Hibernate 查 information_schema.KEYWORDS 失败（HHH000339 仅警告）→ schema 对比无法进行。与 FINDING A（TABLES/PARAMETERS）同源：默认配置（`dolphin.lower_case_table_names=0`）下大小写敏感（视图本身均存在，小写可查） | 高（对按 MySQL 教程配置 ddl-auto=update 的用户是隐性陷阱） | **`dolphin.lower_case_table_names=1` 并持久化（实测 Hibernate 元数据探测恢复）**；未配置时手工建表 + ddl-auto=none |
| FINDING F | COM_STMT_SEND_LONG_DATA 写入含 0x00 的二进制被截断 | 服务端预编译路径对二进制参数按 C 字符串处理：遇第一个 0x00 截断为前缀（200 字节读回 150 = 0x00 位置）；无零字节 1MB 完整往返（MD5 一致）；客户端预编译（文本协议）路径无此问题 | 中（影响服务端预编译 + 二进制大对象写入的正确性） | 服务端预编译路径避免写入含 0x00 的二进制（用客户端预编译文本协议路径） |
| FINDING G | 多语句后续 SELECT 结果集经 getMoreResults() 不可得 | `allowMultiQueries=true` 下结果返回结构不完整：仅回传首语句结果 + 末 DML update count，中间结果全部丢弃（纯 SELECT 序列逐个正常）；各语句 DML 均已落库 | 中（影响依赖多结果集解析的应用/框架） | 逐条执行或仅依赖末条 update count |
| FINDING H | SEND_LONG_DATA 1MB 丢末块 8KB（8.0.28~8.4.0 档，与 #27 报告同源） | 服务端预编译 + setBlob 写入 1MB 全 0x41（无零字节）数据时，8.0.28~8.4.0 档读回仅 1040384 字节 = 127×8192（恰少末块 8192），MD5 失败（断言行固定 `expected: 1048576 but was: 1040384`）；9.7.0/26.7.0 档完整往返。与驱动分块方式（8192 对齐）相关，缺陷在服务端 | 高（服务端预编译 + 大二进制写入数据静默丢失） | 8.0.28~8.4.0 档避免服务端预编译写入 8192 倍数大小的大二进制（改用 9.7.0+ 驱动或客户端预编译文本协议路径） |
| FINDING I | COM_STMT_RESET 崩溃服务端进程（仅 8.0.28 档，与 #27 报告同源） | 8.0.28 档 clearParameters 后二次 executeUpdate（驱动先发 COM_STMT_RESET）直接崩溃 openGauss 服务端：客户端报 `EOFException: read 0 bytes before connection was unexpectedly lost`，服务端日志戛然而止（无 PANIC、无正常关闭流程），3308 不可达需人工重启；单测可复现（两个工程同构）；8.0.33+ 档同一序列正常 | 严重（单次 JDBC 操作即可打崩整个服务端，可被任意客户端触发拒绝服务） | 8.0.28 档避免服务端预编译语句 clearParameters 后复用（新建语句或换 8.0.33+ 驱动）；服务端修复后回归 |

# 6 测试过程评估

## 6.1 测试策略回顾

先以 Boot BOM 默认驱动跑通冒烟建立基线，再针对 ORM 高风险环节（方言/元数据/主键/查询渲染）设计专项用例；对发现的转义语义差异，先在 8.0.28 文档基准驱动上复现以排除驱动因素，再下钻到 SQL 层用 gsql 取证定位服务端语义差异；最后以驱动矩阵确认问题归属。

## 6.2 测试设计评估

1. **已知差异回归测试方法论**：K1 把"当前已知的差异行为"写成断言——将来服务端行为调整后测试自动失败，提示更新文档、移除 workaround，让测试成为文档的守护者；
2. D1 采用手工拉起独立 Spring 容器的写法（SpringApplicationBuilder），避免 @SpringBootTest 在容器异常时把测试直接标 ERROR，从而能观察"启动成功但表未创建"的中间状态——这是捕获 FINDING E 的关键设计；
3. 驱动矩阵与 #27 报告共享同一方法论，两个任务结论互相印证；
4. 协议命令专项（C1-C9）与 #27 报告 7.1-7.9 同一套设计，覆盖 JDBC 可触发的全部协议命令（COM_QUERY/INIT_DB/PING/STMT_*/SET_OPTION/QUIT），无触发路径命令在 4.1.6 说明原因；C5/C8 将 FINDING F/G 的"当前错误行为"写成回归断言（服务端修复后自动失败提醒更新报告），与 K1/D1 同一方法论。

## 6.3 测试执行评估

### 6.3.1 测试执行统计数据

| 项目 | 数据 |
| ---- | ---- |
| 用例总数 | 19 |
| 驱动档位 | 7（含文档基准 8.0.28 与最新 26.7.0） |
| 执行轮次 | 7 档各一轮全量回归（8.0.28 档因 FINDING I 服务端崩溃分两段执行：协议类 9 + 其余 10）+ 若干轮开发调试与单用例复测 |
| 总执行用例次 | 19 × 7 = 133（含 8.0.28 档两段） |

### 6.3.2 测试用例执行结果统计数据

| 结果 | 数量 |
| ---- | ---- |
| 通过 | 122（8.0.28 档 12 / 8.0.33~8.4.0 档各 18 / 9.7.0 档 19 / 26.7.0 档 19） |
| 失败 | 5（FINDING H 断言失败：8.0.28 档 1 + 8.0.33~8.4.0 档各 1） |
| 错误 | 6（FINDING I 服务端崩溃后失联：8.0.28 档 6） |

# 7 附件

## 7.1 附件1：相关产物

| 产物 | 位置 |
| ---- | ---- |
| 测试代码（19 用例） | 本目录 `src/test/java/org/opengauss/smoke/jpacases/` |
| 测试思维导图 | 本目录 `docs/测试思维导图-SpringDataJPA连接openGaussB库.{mm,md,html}` |
| 转义语义差异取证材料 | 本报告 7.2 节（gsql 三条取证 SQL 输出，服务端直连复现，不依赖 Java 组件） |
| 指导文档 | docs 仓 `docs/zh/mysql_compatibility_developer_guide/springdatajpa_development.md`（轻量版 `docs-lite/zh/...` 同路径） |

## 7.2 附件2：LIKE escape 转义语义差异 gsql 取证输出（与 Plugin 仓 issue #504 同源）

> 取证环境：openGauss 7.0.0-RC3（commit fe6f6e4b，debug 编译），B 库 proto_test_db，通过 gsql（postgres 协议 5432）直连复现——不依赖任何 Java 组件，与驱动版本无关。

```sql
proto_test_db=# SELECT '\\';
 \\
----
 \\
(1 row)

proto_test_db=# SELECT 'abc' LIKE 'a%' ESCAPE '\\';
ERROR:  invalid escape string
HINT:  Escape string must be empty or one character.

proto_test_db=# SELECT 'abc' LIKE 'a%' ESCAPE '!';
 ?column?
----------
 t
(1 row)
```

三条 SQL 说明：① openGauss B 模式按标准 SQL 解释字符串字面量，`'\\'` 是两个字符的反斜杠（MySQL 反斜杠转义语义下是单字符）；② LIKE 的 ESCAPE 子句要求转义符为空或单字符，两字符的 `'\\'` 触发 `invalid escape string`；③ 换用单字符 `!` 作为转义符即可正常执行。Hibernate 6.6 默认给 LIKE 追加 `escape '\\'`，故派生模糊查询（如 `findByNameLike`）在 B 模式下全部报错（该差异的回归用例见 4.1.1 节），JPQL 显式 `escape '!'` 可规避。该差异的根源是 B 模式不支持反斜杠作为字符串转义字符，与社区已记录的已知差异同源（Plugin 仓 issue #504）。
