# Spring Boot (JDBC) 连接 openGauss B 兼容库

对应任务：Issue #27 — openGauss B 库 B 协议适配 MySQL Java 框架 Spring Boot。

使用 Spring Boot 3.x 配合 MySQL JDBC 驱动连接 openGauss B 兼容模式数据库的示例工程，包含七维度 37 个测试用例、8 档驱动版本矩阵，以及测试思维导图与自测报告。

## 目录结构

```
springboot-connect-opengauss-b
├── pom.xml                    # 含 8 档驱动 profile（版本矩阵用）
├── README.md
├── docs
│   ├── 测试思维导图-SpringBoot连接openGaussB库.{mm,md,html}
│   └── 基于SpringBoot框架的openGauss数据库测试.md   # 自测报告
└── src
    ├── main/java/org/opengauss/smoke      # 示例应用（SmokeApplication）
    └── test/java/org/opengauss/cases      # 七维度测试套件（37 用例）
```

## 前置条件

1. 开发机：JDK 17+、Maven 3.6+（Spring Boot 3.x 要求 Java 17）
2. openGauss 服务端（openEuler + openGauss 7.0.0，开启 MySQL 协议兼容）：
   - `enable_dolphin_proto = on`、`dolphin_server_port = 3308`
   - 已创建 B 库 `proto_test_db`、schema `mysql_test_db`、业务表 `` `user` ``
   - 用户 `mysql_test_db` 已 `set_native_password` 并赋权，pg_hba 已放行
3. 网络：开发机可访问 `<VM_IP>:3308`（防火墙放行）

环境搭建步骤详见 docs 仓《[Spring Boot 连接示例](https://docs.opengauss.org/zh/docs/latest/mysql_compatibility_developer_guide/springboot_development.html)》。

## 配置

通过环境变量覆盖连接信息：

| 变量 | 默认值 | 说明 |
|---|---|---|
| DB_HOST | 127.0.0.1 | openGauss 所在机器的 IP |
| DB_PORT | 3308 | dolphin 协议端口（不是 5432 原生口） |
| DB_NAME | mysql_test_db | B 库下 schema（等价 MySQL database） |
| DB_USER / DB_PASSWORD | mysql_test_db / ChangeMe_123 | MySQL 认证用户 |

## 运行

```bash
# 运行全部测试（37 用例）
mvn test

# 运行示例主程序
mvn spring-boot:run
```

## 测试内容（七维度 37 用例）

| 维度 | 用例数 | 覆盖点 |
|---|---|---|
| 连接与认证 | 4 | 正常连接/错误密码/错误用户/5432 原生口拒绝 + 元数据信息 |
| 类型映射 | 8 | 9 种类型往返 + tinyInt1isBit/transformedBitIsBoolean/zeroDateTimeBehavior/useOldAliasMetadataBehavior/useaffectedrows/allowMultiQueries 六个文档参数 |
| DML 批量 | 3 | addBatch 1000 行 / JdbcTemplate / rewriteBatchedStatements 提速对比 |
| 事务 | 5 | TransactionTemplate 提交回滚 / 手动事务 / SAVEPOINT / 隔离级别 |
| 连接池 | 2 | HikariCP 配置生效 / CONNECTION_ID() 验证复用 |
| 已知限制 | 3 | 游标 / Statement.setFetchSize / 存储过程 |
| 协议命令 | 9 | COM_QUERY/INIT_DB/PING/STMT_PREPARE/STMT_EXECUTE/STMT_CLOSE/STMT_SEND_LONG_DATA/STMT_RESET/STMT_FETCH/SET_OPTION/QUIT（评审意见落地） |

## 驱动版本矩阵（8 档 × 37 用例，协议命令行为分三档）

| 驱动版本 | 坐标 | 结果 |
|---|---|---|
| 8.0.28（文档基准） | mysql:mysql-connector-java（旧坐标） | 协议类 1 失败 + 6 错误（FINDING I 崩溃服务端 + FINDING H 丢 8KB），其余 30 通过 |
| 8.0.33 / 8.1.0 / 8.3.0 / 8.4.0 | com.mysql:mysql-connector-j（新坐标） | 36/37 × 4（仅 FINDING H：SEND_LONG_DATA 丢末块 8KB） |
| 9.7.0（Boot 3.5.16 BOM 默认） | 同上 | 37/37 |
| 26.7.0（最新） | 同上 | 37/37 |

切换档位重跑：

```bash
mvn test -Pdriver-8.0.28     # 文档基准（旧坐标；注意 FINDING I 会打崩服务端，需重启）
mvn test -Pdriver-8.0.33
mvn test -Pdriver-8.1.0
mvn test -Pdriver-8.3.0
mvn test -Pdriver-8.4.0
mvn test -Pdriver-9.7.0
mvn test -Pdriver-26.7.0
```

> 基础功能（CRUD/事务/批量/类型映射/连接池）8 档逐项一致；协议命令专项按驱动版本分三档：8.0.28 崩 / 8.0.33~8.4.0 丢数据 / 9.7.0+ 正常（FINDING H/I，缺陷在服务端）。详见 docs/ 自测报告 4.2 节。

## 已知限制与 FINDING

| 项 | 说明 |
|---|---|
| FINDING A | `getTables` 报错 information_schema.TABLES 不存在（默认配置 `dolphin.lower_case_table_names=0` 下大小写敏感：大写报错、小写可查；设置 =1 并持久化后实测恢复） |
| FINDING B | 零日期由 `dolphin.sql_mode` 的 `no_zero_date` 控制：默认开启写入被拒，去掉后可存储；存储后 zeroDateTimeBehavior 三档读取均生效（convertToNull→null / exception→抛错 / round→0001-01-01） |
| FINDING C（已改写为正向确认） | useOldAliasMetadataBehavior 在表列别名场景参数生效（true 时 getColumnName 返回别名）；表达式别名两种设置一致是文档规定的预期行为 |
| FINDING D | SERIALIZABLE 被映射为 REPEATABLE_READ |
| FINDING F | COM_STMT_SEND_LONG_DATA（服务端预编译）写入含 0x00 的二进制被按 C 字符串截断为前缀（无零字节 1MB 完整往返 MD5 一致；客户端预编译路径无此问题） |
| FINDING G | allowMultiQueries 多语句后续 SELECT 结果集经 getMoreResults() 不可得（仅首语句结果 + 末 DML update count；DML 均落库） |
| FINDING H | 服务端预编译 + SEND_LONG_DATA 写入 1MB 全 0x41 数据时，8.0.28~8.4.0 档读回仅 1040384 字节（= 127×8192，丢末块 8KB）；9.7.0/26.7.0 档完整往返 |
| FINDING I | 8.0.28 档 clearParameters 后二次执行（COM_STMT_RESET）可复现崩溃 openGauss 服务端进程（EOFException 断连、3308 不可达、需重启）；8.0.33+ 档正常 |
| 游标 | 驱动层无报错，协议层是否真游标待确认 |
| 存储过程 | prepareCall 报错 PARAMETERS 不存在（默认配置大小写敏感所致，`lower_case_table_names=1` 后大写可查；文档已声明不支持） |

完整分析见 `docs/基于SpringBoot框架的openGauss数据库测试.md`。

> Maven 依赖拉取失败时，在 `~/.m2/settings.xml` 配置阿里云镜像：
> `<mirror><id>aliyun</id><mirrorOf>central</mirrorOf><url>https://maven.aliyun.com/repository/central</url></mirror>`
