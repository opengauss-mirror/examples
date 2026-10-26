# Spring Boot + DBCP 连接 openGauss B 兼容模式快速开始

## 环境要求

- JDK 8+、Maven 3.6+
- Apache Commons DBCP2 2.9.0、Commons Pool2 2.10.0
- MySQL Connector/J 8.0.28
- 包含 Dolphin 插件并已启用 MySQL 协议的 openGauss

## 操作步骤

1. 在 `postgresql.conf` 中配置并重启实例：

   ```ini
   shared_preload_libraries = 'dolphin'
   enable_dolphin_proto = on
   dolphin_server_port = 3308
   dolphin.default_database_name = 'proto_test_db'
   ```

2. 确认 Dolphin 端口监听：

   ```bash
   ss -tlnp | grep 3308
   ```

3. 将 `src/main/resources/create_b_database.sql` 中的 `<DBCP_PASSWORD>` 替换为测试密码，
   然后初始化数据库：

   ```bash
   gsql -d postgres -h "$PGDATA" -p 5432 \
     -f src/main/resources/create_b_database.sql
   gsql -d proto_test_db -h "$PGDATA" -p 5432 \
     -f src/main/resources/init.sql
   ```

4. 运行 Spring Boot 链路示例：

   ```bash
   DBCP_PASSWORD='实际密码' mvn spring-boot:run
   ```

   日志应显示实际数据源为 `org.apache.commons.dbcp2.BasicDataSource`，并显示
   `JdbcTemplate` 查询、事务提交和回滚结果。

5. 运行 46 项低层测试和普通 JDBC 示例：

   ```bash
   mvn clean test -Ddbcp.password="$DBCP_PASSWORD"
   mvn exec:java -Dexec.mainClass=org.opengauss.DbcpConnectDemo
   ```

预期结果：`Tests run: 46, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

完整说明见 `../README.md`、`../docs/初始化步骤.md` 和 `../docs/自测报告.md`。
