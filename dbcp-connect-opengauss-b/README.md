# DBCP 连接 openGauss B 兼容模式示例

本项目演示 Apache Commons DBCP2 通过 MySQL Connector/J 和 Dolphin MySQL 协议端口
访问 openGauss B 兼容模式数据库，并提供连接池、CRUD、事务、并发和 SQL 分类测试。

## 环境要求

| 组件 | 版本或要求 |
|---|---|
| JDK | 8+ |
| Maven | 3.6+ |
| Apache Commons DBCP2 | 2.9.0 |
| Apache Commons Pool2 | 2.10.0（DBCP2 传递依赖） |
| MySQL Connector/J | 8.0.28 |
| openGauss | 包含 Dolphin 插件并启用 MySQL 协议 |

DBCP2 2.9.0 和 Connector/J 8.0.28 的核心类使用 Java 8 字节码，因此最低运行版本为
JDK 8。openGauss 服务端与 Dolphin 应使用同一包含 Dolphin 的安装包或转测包，直接按包内
配置启动即可，无需单独构建 Dolphin。

本文命令默认使用 Dolphin 端口 `3308`。自测报告使用独立实例端口 `36443`，通过
`-Ddbcp.url` 覆盖连接地址；端口不同只代表实例隔离，连接链路和测试内容相同。

## 连接链路

```text
dbcp.properties
  -> BasicDataSourceFactory
  -> BasicDataSource.getConnection()
  -> GenericObjectPool.borrowObject()
  -> PoolableConnectionFactory.makeObject()（池中无空闲连接时）
  -> DriverConnectionFactory.createConnection()
  -> com.mysql.cj.jdbc.Driver.connect()
  -> com.mysql.cj.jdbc.ConnectionImpl
  -> TCP / Dolphin MySQL 协议端口
  -> openGauss B 兼容库
  -> ResultSet 或 updateCount
  -> PoolableConnection.close()
  -> GenericObjectPool.returnObject() 将连接归还 DBCP
```

`BasicDataSource` 管理池参数和连接生命周期。仅当池内没有可复用的空闲连接且未达到
`maxTotal` 时，DBCP 才调用 Connector/J 创建物理连接；Connector/J 负责 MySQL 协议通信，
Dolphin 将协议请求交给 openGauss 执行。业务代码调用 `Connection.close()` 时，DBCP
归还逻辑连接；物理连接是否保留由连接池配置决定。

## Spring Boot 快速开始

工程同时提供一个可直接运行的 Spring Boot 入口，用于验证真实的应用链路：

```text
SpringApplication.run
  -> Spring Boot DataSourceAutoConfiguration
  -> BasicDataSource（DBCP2）
  -> JdbcTemplate / TransactionTemplate
  -> MySQL Connector/J
  -> Dolphin MySQL 协议
  -> openGauss B 兼容库
```

`spring-boot-starter-jdbc` 默认会选择 HikariCP。本工程在 `pom.xml` 排除了 HikariCP，
并在 `application.properties` 显式指定 `BasicDataSource`，因此运行时不会误用其他连接池。
完成数据库初始化后执行：

```bash
export DBCP_PASSWORD='实际密码'
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-DDBCP_PASSWORD=$DBCP_PASSWORD"
```

也可以通过环境变量覆盖地址和用户：

```bash
DBCP_URL='jdbc:mysql://127.0.0.1:3308/mysql_test_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
DBCP_USER=opengauss DBCP_PASSWORD='实际密码' mvn spring-boot:run
```

启动日志应包含 `BasicDataSource`、连接池参数、`JdbcTemplate` 查询行数以及
`TransactionTemplate COMMIT/ROLLBACK` 结果。示例会删除自己插入的临时数据。
已在 VM 的 master 分支转测包实例（Dolphin 36443）上验证通过；使用专用账号时请确保同时授予
schema、表和自增序列权限。

## 普通 JDBC 示例与测试

### 1. 配置 Dolphin

在 `postgresql.conf` 中设置：

```ini
shared_preload_libraries = 'dolphin'
enable_dolphin_proto = on
dolphin_server_port = 3308
dolphin.default_database_name = 'proto_test_db'
```

重启实例并确认端口监听：

```bash
gs_ctl restart -D "$PGDATA" -Z single_node -l "$GAUSSLOG/restart.log"
ss -tlnp | grep 3308
```

### 2. 配置客户端认证

以下配置仅允许本机访问。远程连接时应改为实际客户端所在的受限网段。

```ini
local   all all              trust
host    all all 127.0.0.1/32 sha256
host    all all ::1/128      sha256
```

修改 `pg_hba.conf` 后执行：

```bash
gs_ctl reload -D "$PGDATA"
```

### 3. 初始化数据库

先将 `src/main/resources/create_b_database.sql` 中的 `<DBCP_PASSWORD>` 替换为测试密码，
再按以下顺序执行脚本：

```bash
gsql -d postgres -h "$PGDATA" -p 5432 \
  -f src/main/resources/create_b_database.sql
gsql -d proto_test_db -h "$PGDATA" -p 5432 \
  -f src/main/resources/init.sql
```

脚本创建 B 兼容库 `proto_test_db`、schema `mysql_test_db`、`user` 表和初始数据。

### 4. 配置连接池

编辑 `src/main/resources/dbcp.properties`：

```properties
driverClassName=com.mysql.cj.jdbc.Driver
url=jdbc:mysql://127.0.0.1:3308/mysql_test_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf-8&zeroDateTimeBehavior=CONVERT_TO_NULL
username=opengauss
password=<DBCP_PASSWORD>

initialSize=2
maxTotal=8
maxIdle=8
minIdle=2
maxWaitMillis=5000

validationQuery=SELECT 1
testOnBorrow=true
testWhileIdle=true
timeBetweenEvictionRunsMillis=60000
minEvictableIdleTimeMillis=300000
```

### 5. 运行测试和示例

测试密码也可以通过系统属性传入，避免修改配置文件：

```bash
mvn clean test -Ddbcp.password="$DBCP_PASSWORD"
mvn exec:java -Dexec.mainClass=org.opengauss.DbcpConnectDemo
```

全量测试的预期结果为：

```text
Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 连接池边界

`maxTotal=8` 表示单个数据源最多同时借出 8 条连接。连接池满后，新请求最多等待
`maxWaitMillis`；等待期间不得突破 `maxTotal`。连接归还后，排队请求应继续执行；等待
超时则抛出 `SQLException`。

`ConnectionPoolTest#testMaxTotalConcurrentCrudAndQueuedBorrow` 使用 8 条连接分别执行
`INSERT -> SELECT -> UPDATE -> SELECT -> DELETE -> COMMIT` 并保持占用，同时验证第 9 条
请求等待、超时和释放后的恢复行为。

`StabilityTest#testConcurrentCrudLoad` 使用 16 个并发连接执行完整事务 CRUD，并在结束时
断言活动连接数归零。更高负载测试固定 `maxTotal=128`，验证了 128 条同时持有连接和最多
5000 个排队业务请求；请求数不等于物理连接数。

## 测试范围

| 测试类 | 用例数 | 主要内容 |
|---|---:|---|
| `DbcpDataSourceTest` | 5 | 配置装配、驱动加载、初始连接及错误连接参数 |
| `ConnectionPoolTest` | 10 | 借出归还、池参数、并发、满池等待与恢复 |
| `ConnectionValidationTest` | 3 | `testOnBorrow`、`testWhileIdle` 和断连恢复 |
| `CrudTest` | 8 | 查询、插入、更新、删除、分页和批处理 |
| `TransactionTest` | 5 | 自动提交、提交、回滚、隔离级别和池化连接事务 |
| `SqlCategoryTest` | 5 | DDL、DML、DQL、DCL、TCL |
| `DataTypeCharsetTest` | 3 | 中文、emoji、常用类型和 NULL |
| `ConstraintTest` | 3 | 多语句限制、游标参数和非 SSL 连接 |
| `StabilityTest` | 4 | 重复建池、并发事务、长连接和池状态 |
| **合计** | **46** | |

DDL、DML、DQL、DCL、TCL 用例分别校验对象状态、影响行数、查询结果、授权状态以及
事务提交和回滚后的数据可见性。examples 的 DCL 测试使用具备 `CREATEROLE` 权限的
`opengauss` 测试账号；生产部署应改用专用业务账号，并按实际权限调整 DCL 用例。

## 相关文档

- `docs/初始化步骤.md`：完整初始化命令。
- `docs/测试思维导图.png`：测试范围思维导图（用于在线预览）。
- `docs/测试思维导图.mm`：可编辑的 FreeMind 思维导图源文件。
- `docs/自测报告.md`：测试环境、结果和用例映射。
