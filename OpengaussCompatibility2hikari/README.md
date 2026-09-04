# spring-boot-ospp

## 验证总览

| 序号 | 验证项 | 为什么验证 | 是否通过 |
| --- | --- | --- | --- |
| 1 | Spring Boot 自动装配 HikariCP | 确认 Spring Boot 2.5.6 引入 `spring-boot-starter-jdbc` 后，实际使用的是 `HikariDataSource`，不是其他连接池。 | 通过 |
| 2 | HikariCP 5.x 运行版本 | 确认工程没有使用 Spring Boot 2.5.6 默认的 HikariCP 4.x，而是显式覆盖到 HikariCP 5.1.0。 | 通过 |
| 3 | HikariCP 连接池参数 | 确认 `maximumPoolSize`、`minimumIdle`、`connectionTimeout`、`idleTimeout`、`maxLifetime`、`keepaliveTime` 等配置已被读取并生效。 | 通过 |
| 4 | MySQL JDBC 基础连接 | 确认 MySQL Connector/J 可以通过 dolphin MySQL 协议端口连接 openGauss，并通过 `isValid`、`SELECT 1`、`version()` 验证基础 SQL 能力。 | 通过 |
| 5 | dolphin 插件与协议配置 | 通过 `pg_extension` 验证 dolphin 插件已安装，通过 `pg_settings` 验证 `enable_dolphin_proto=on`、`dolphin_server_port=3306`。 | 通过 |
| 6 | 客户端库名与 schema 映射 | 确认 JDBC URL 中的 `mysql_test_db` 能映射到 openGauss 当前 schema，避免文档中库名/schema 映射说明不严谨。 | 通过 |
| 7 | 初始业务表数据 | 确认文档准备步骤创建的 `user` 表可查，并且包含 `张三`、`李四`、`王五` 三条基础数据。 | 通过 |
| 8 | INSERT / UPDATE / DELETE | 确认通过 MySQL 协议连接 openGauss 后，常规写入、更新、删除链路都可用。 | 通过 |
| 9 | Spring 事务提交 | 确认 `TransactionTemplate` 能基于 HikariCP 获取的连接正常提交事务，并且提交后的数据可查询。 | 通过 |
| 10 | 连接池最大连接数限制 | 借满 `maximumPoolSize=10` 条连接后，再申请第 11 条连接，确认它会等待空闲连接，不会突破连接池最大连接数。 | 通过 |
| 11 | 连接池等待恢复 | 释放 1 条已占用连接后，确认等待中的第 11 条连接可以恢复执行并完成 SQL。 | 通过 |
| 12 | 真实并发数据库操作 | 启动 10 个 worker，每个 worker 持有自己的连接并完成 `insert/select/update/select/delete/commit`，证明不是只拿到连接就算并发通过。 | 通过 |
| 13 | 最终清理 | 删除验证过程产生的临时数据，确认基础数据仍保留，保证重复运行结果可控。 | 通过 |

Spring Boot 2.5.6 + HikariCP + MySQL Connector/J 验证工程，用于按 openGauss docs 的《基于 HikariCP 开发》验证 openGauss B 兼容库的 MySQL 协议连接能力。

## 环境

- JDK 11
- Spring Boot 2.5.6
- MySQL Connector/J 8.0.20
- HikariCP 5.1.0，通过 `hikaricp.version` 显式覆盖 Spring Boot 2.5.6 默认依赖版本
- Spring Boot `spring-boot-starter-jdbc` 默认连接池 HikariCP

## openGauss 侧准备

按文档完成服务端准备：

1. 开启 MySQL 协议兼容，确认 `enable_dolphin_proto=on`。
2. 创建 B 兼容库并加载 dolphin，示例库名为 `proto_test_db`。
3. 设置 `dolphin_server_port=3306` 并重启。
4. 在 B 库中创建 schema 和 `user` 表，schema 名为 `mysql_test_db`。
5. 创建同名连接用户 `mysql_test_db`，并执行 `SELECT set_native_password('mysql_test_db', '<password>', '');`。
6. 配置客户端接入认证，例如 `host all mysql_test_db 0.0.0.0/0 sha256`。

## 运行验证

默认会直接连接本机 openGauss dolphin MySQL 协议端口并执行验证：

```powershell
mvn spring-boot:run
```

也可以直接在 IDEA 中运行 `com.linyu.ospp.SpringBootOsppApplication`。

默认连接信息：

```text
jdbc:mysql://127.0.0.1:3306/mysql_test_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf-8&allowPublicKeyRetrieval=true
username=mysql_test_db
password=xxxxxx
```

如需覆盖连接信息，可设置环境变量：

```powershell
$env:OPENGAUSS_MYSQL_URL="jdbc:mysql://127.0.0.1:3306/mysql_test_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf-8&allowPublicKeyRetrieval=true"
$env:OPENGAUSS_MYSQL_USERNAME="mysql_test_db"
$env:OPENGAUSS_MYSQL_PASSWORD="xxxxxx"
mvn spring-boot:run
```

验证内容：

- Spring Boot 是否自动装配 `HikariDataSource`
- HikariCP 运行版本是否为 5.x
- HikariCP 参数是否生效
- `SELECT 1` 和 `version()` 是否可执行
- `pg_extension` 中是否已安装 dolphin 插件，`enable_dolphin_proto` / `dolphin_server_port` 是否符合预期
- dolphin MySQL 协议下客户端库名 `mysql_test_db` 是否映射到当前 schema
- 文档中的 `user` 表是否可查询
- INSERT / UPDATE / DELETE 是否可执行
- Spring 事务提交是否正常
- 连接数超过 HikariCP `maximumPoolSize` 后是否进入等待，释放连接后是否恢复执行
- 10 个并发 worker 是否能各自持有连接并完成 insert/select/update/select/delete/commit

看到如下结尾表示通过：

```text
验证通过：Spring Boot 2.5.6 + HikariCP + MySQL Connector/J 可以通过 dolphin MySQL 协议访问 openGauss，并完成查询、增删改、事务、连接池上限和真实并发数据库操作验证。
```

## 连接链路与协议转换

本工程一次业务 SQL 的链路分为应用侧、驱动侧与服务端侧三段。

应用侧，Spring Boot 引入 `spring-boot-starter-jdbc` 后在类路径存在 HikariCP 时自动装配 `HikariDataSource`。业务通过 `DataSource.getConnection()` 获取连接，实际进入 `HikariPool.getConnection()`。HikariPool 用 `ConcurrentBag` 管理物理连接，存在空闲连接时复用，未达到 `maximumPoolSize` 时新建，达到上限后按 `connectionTimeout` 阻塞等待。新建物理连接时，HikariCP 调用 JDBC 驱动的 `connect` 方法。

驱动侧，MySQL Connector/J 的 `com.mysql.cj.jdbc.Driver.connect()` 建立到 `host:3306` 的 TCP 连接，按 MySQL 客户端服务端协议完成握手与认证。认证使用 B 库用户的 MySQL 原生密码，即通过 `set_native_password` 设置的密码，因此连接串需要 `allowPublicKeyRetrieval=true` 以支持公钥检索。连接建立后，HikariCP 会执行连接初始化探测，其中一项是查询事务隔离级别，例如执行 `SELECT @@session.transaction_isolation`。

服务端侧，openGauss 在 B 兼容库开启 dolphin 后，由 dolphin 插件在 `dolphin_server_port` 指定的端口（本工程设为 3306，需与 openGauss 自身 `port` 不同）监听 MySQL 协议。该监听的前置条件是 GUC 参数 `enable_dolphin_proto` 设为 on，且修改后需重启数据库生效。dolphin 通过抽象协议层接口，将收到的 MySQL 协议报文转换为 openGauss 可识别的逻辑执行，再把结果按 MySQL 协议格式封装返回。业务 SQL（`SELECT 1`、CRUD、事务提交）都经由同一条链路。

连接池只管理物理连接生命周期，真正的兼容边界在 MySQL 协议与 dolphin 的翻译层。只要 Connector/J 能与 dolphin 正常完成协议握手与系统变量探测，连接池层不会引入额外兼容问题。连接初始化阶段的隔离级别探测曾因早期 dolphin 返回 `default` 导致 HikariCP 建连失败，该问题在 openGauss 7.0.0-RC3（dolphin 5.2）已修复。更完整的链路与代码节点说明见 docs 仓《基于 HikariCP 开发》。

## 注意

openGauss 默认 `session_timeout` 通常为 10 分钟，因此本工程将 HikariCP `max-lifetime` 配为 540000 毫秒，小于服务端默认会话超时。

## 五类标准 SQL 操作验证

除上述 13 项 Spring Boot 集成验证外，工程还提供独立的五类标准 SQL 操作验证程序 `SqlCategoryVerificationRunner.java`，覆盖 DDL / DML / DQL / DCL / TCL 全部操作类别及并发连接测试，不依赖 Spring Boot 框架（纯 HikariCP + JDBC）。

### 验证分类

| 分类 | 核心关键字 | 验证项数 |
|------|-----------|---------|
| DDL | CREATE, ALTER, DROP, TRUNCATE, CTAS, INDEX | 7 |
| DML | INSERT（单行+批量）, UPDATE, DELETE, RETURNING | 5 |
| DQL | SELECT, WHERE, ORDER BY, 聚合函数, GROUP BY, DISTINCT, 子查询 | 7 |
| DCL | GRANT, REVOKE, 权限查询 | 4 |
| TCL | COMMIT, ROLLBACK, SAVEPOINT, 隔离级别 | 4 |
| 并发 | 10 worker 同时持连接执行完整事务 | 1 |

### 运行方式

```powershell
# 编译
mvn dependency:copy-dependencies -DoutputDirectory=target\lib -q
javac -encoding UTF-8 -cp "target\lib\*" -d target\classes src\main\java\com\linyu\ospp\SqlCategoryVerificationRunner.java

# 运行（需 JDK 11+）
java -cp "target\classes;target\lib\*" com.linyu.ospp.SqlCategoryVerificationRunner
```

### 自验结果

本次实测共 28 项验证，其中 26 项 PASS，2 项 INFO。INFO 项为 DML-05 getGeneratedKeys 与 TCL-04 隔离级别查询，两者均为只读能力探测，不计入 PASS。分类统计为 DDL 7 项、DML 4 项 PASS 加 1 项 INFO、DQL 7 项、DCL 4 项、TCL 3 项 PASS 加 1 项 INFO、并发 1 项。

实测输出见 `sql_category_verification.txt`，自验清单见 `五类SQL操作自验清单.txt`。
