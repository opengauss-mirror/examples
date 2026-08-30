package com.linyu.ospp;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class HikariVerificationRunner implements CommandLineRunner {
    // user 是 MySQL 关键字，按文档使用反引号保持与 openGauss B 兼容语法一致。
    private static final String TABLE_NAME = "`user`";
    private static final List<String> REQUIRED_INITIAL_USERS = Arrays.asList("张三", "李四", "王五");
    private static final String REQUIRED_HIKARI_VERSION_PREFIX = "5.";
    private int sectionNo = 1;

    private final boolean enabled;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public HikariVerificationRunner(@Value("${ospp.verify.enabled:false}") boolean enabled,
                                    DataSource dataSource,
                                    JdbcTemplate jdbcTemplate,
                                    TransactionTemplate transactionTemplate) {
        this.enabled = enabled;
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        // 测试类中会关闭该开关，避免普通 mvn test 必须依赖本机 openGauss 实例。
        // 直接运行 main 时 application.yml 默认开启，方便一键验证文档步骤。
        if (!enabled) {
            System.out.println("OSPP HikariCP 验证已关闭；如需运行，请设置 OSPP_VERIFY_ENABLED=true。");
            return;
        }

        printSection("验证 HikariCP 数据源自动装配");
        printPurpose("确认 Spring Boot 2.5.6 通过 spring-boot-starter-jdbc 自动创建 HikariDataSource，并读取到预期连接池参数。");
        HikariDataSource hikariDataSource = verifyDataSourceInfo();

        printSection("验证数据库连接与基础 SQL");
        printPurpose("确认 MySQL Connector/J 可以通过 dolphin MySQL 协议端口连接 openGauss，并执行基础查询。");
        verifyConnection();

        printSection("验证 dolphin 插件与 MySQL 协议配置");
        printPurpose("直接查询 openGauss 系统表，确认 dolphin 插件已经安装，并确认 dolphin MySQL 协议开关和监听端口配置存在。");
        verifyDolphinPlugin();

        printSection("验证 dolphin 客户端库名与 schema 映射");
        printPurpose("确认 JDBC URL 中的 mysql_test_db 能映射到 openGauss B 兼容库中的 mysql_test_db schema，避免文档只写库名但实际落错 schema。");
        verifySchemaMapping();

        printSection("清理历史验证数据");
        printPurpose("删除上一次验证遗留的临时数据，保证本次 CRUD、事务、并发验证结果可重复。");
        cleanupPreviousRunRows();

        printSection("验证初始业务表查询");
        printPurpose("确认文档准备步骤中的 user 表可以通过 MySQL 协议查询，并且至少包含张三、李四、王五三条基础数据。");
        verifyInitialUsers();

        printSection("验证 INSERT 插入能力");
        printPurpose("向 user 表插入一条临时数据，确认 MySQL 协议下的写入链路可用。");
        insertUser("zhaoliu", 18);
        selectUsers("插入后");

        printSection("验证 UPDATE 更新能力");
        printPurpose("按 name 找到刚插入的数据并更新，确认 MySQL 协议下的更新链路可用。");
        Integer id = getUserIdByName("zhaoliu");
        if (id == null) {
            throw new IllegalStateException("插入后的 zhaoliu 数据没有查到，INSERT 验证结果不可信。");
        }

        updateUser(id, "zhaoliuliuliu", 28);
        assertUserExists("zhaoliuliuliu", 28);
        selectUsers("更新后");

        printSection("验证 Spring 事务提交");
        printPurpose("通过 TransactionTemplate 插入并提交一条数据，确认 Spring 事务管理器和 Hikari 连接协同正常。");
        verifyTransaction();

        printSection("验证连接数超出 Hikari 配置后的等待行为");
        printPurpose("先借满 maximumPoolSize 条连接，再申请第 maximumPoolSize + 1 条连接，确认 HikariCP 不会突破最大连接数，而是等待空闲连接。");
        verifyPoolLimit(hikariDataSource);

        printSection("验证并发真实数据库操作");
        printPurpose("启动 10 个 worker，每个 worker 持有自己的连接，并完成 insert/select/update/select/delete/commit，证明不是只拿到连接就算并发通过。");
        verifyConcurrentOperations();

        printSection("验证 DELETE 删除能力与最终清理");
        printPurpose("删除本次单线程 CRUD 临时数据，并清理事务、并发、连接数验证产生的临时数据。");
        deleteUser(id);
        cleanupPreviousRunRows();
        selectUsers("最终清理后");

        printSection("验证结论");
        System.out.println("验证通过：Spring Boot 2.5.6 + HikariCP + MySQL Connector/J 可以通过 dolphin MySQL 协议访问 openGauss，并完成查询、增删改、事务、连接池上限和真实并发数据库操作验证。");
    }

    private void printSection(String title) {
        System.out.println();
        System.out.println("==================== " + sectionNo++ + ". " + title + " ====================");
    }

    private void printPurpose(String purpose) {
        System.out.println("测试目的：" + purpose);
    }

    private HikariDataSource verifyDataSourceInfo() {
        // Spring Boot 2.x 在引入 spring-boot-starter-jdbc 后默认选择 HikariCP。
        // 这里显式断言数据源类型，防止项目后续依赖变化导致连接池被替换。
        if (!(dataSource instanceof HikariDataSource)) {
            throw new IllegalStateException("数据源类型错误：预期是 HikariDataSource，实际是 " + dataSource.getClass().getName());
        }

        HikariDataSource hikari = (HikariDataSource) dataSource;
        System.out.println("数据源实现类：" + hikari.getClass().getName());
        String hikariVersion = resolveHikariVersion();
        System.out.println("HikariCP 运行版本：" + hikariVersion);
        System.out.println("HikariCP 加载位置：" + resolveHikariLocation());
        System.out.println("连接池名称：" + hikari.getPoolName());
        System.out.println("最大连接数 maximumPoolSize：" + hikari.getMaximumPoolSize());
        System.out.println("最小空闲连接数 minimumIdle：" + hikari.getMinimumIdle());
        System.out.println("获取连接超时时间 connectionTimeout(ms)：" + hikari.getConnectionTimeout());
        System.out.println("空闲连接保留时间 idleTimeout(ms)：" + hikari.getIdleTimeout());
        System.out.println("连接最大生命周期 maxLifetime(ms)：" + hikari.getMaxLifetime());
        System.out.println("连接保活时间 keepaliveTime(ms)：" + hikari.getKeepaliveTime());

        if (hikari.getMaximumPoolSize() <= 0) {
            throw new IllegalStateException("HikariCP 最大连接数配置无效：" + hikari.getMaximumPoolSize());
        }
        if (hikariVersion == null || !hikariVersion.startsWith(REQUIRED_HIKARI_VERSION_PREFIX)) {
            throw new IllegalStateException("HikariCP 版本校验失败：预期运行 5.x 版本，实际为 " + hikariVersion);
        }
        if (hikari.getMaxLifetime() <= hikari.getKeepaliveTime()) {
            throw new IllegalStateException("HikariCP maxLifetime 应大于 keepaliveTime，否则连接保活配置没有意义。");
        }
        System.out.println("数据源装配校验通过：当前使用 HikariCP，连接池参数已生效。");
        return hikari;
    }

    private String resolveHikariVersion() {
        String implementationVersion = HikariDataSource.class.getPackage().getImplementationVersion();
        if (implementationVersion != null) {
            return implementationVersion;
        }

        // mvn spring-boot:run 使用展开 classpath 时，Manifest 版本可能读不到；
        // 此时从 HikariCP jar 文件名中解析版本，仍然能证明运行时加载的是哪个版本。
        String location = resolveHikariLocation();
        String marker = "HikariCP-";
        int versionStart = location.indexOf(marker);
        int jarSuffix = location.indexOf(".jar", versionStart);
        if (versionStart >= 0 && jarSuffix > versionStart) {
            return location.substring(versionStart + marker.length(), jarSuffix);
        }
        return null;
    }

    private String resolveHikariLocation() {
        try {
            return HikariDataSource.class.getProtectionDomain().getCodeSource().getLocation().toString();
        } catch (Exception e) {
            return "无法读取：" + e.getMessage();
        }
    }

    private void verifyConnection() throws Exception {
        // 同时验证 JDBC4 isValid、简单 SELECT、version()，覆盖连接池到数据库的最短链路。
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(5);
            System.out.println("JDBC 连接有效性校验结果：" + valid);
            if (!valid) {
                throw new IllegalStateException("JDBC 连接有效性校验失败。");
            }
        }

        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        System.out.println("基础 SQL 执行结果：SELECT 1 = " + one);
        if (one == null || one != 1) {
            throw new IllegalStateException("基础 SQL 校验失败：SELECT 1 返回值为 " + one);
        }

        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
        System.out.println("数据库版本信息：" + version);
        if (version == null || !version.toLowerCase().contains("opengauss")) {
            throw new IllegalStateException("数据库版本信息中没有识别到 openGauss，实际返回：" + version);
        }
        System.out.println("数据库连接与基础 SQL 校验通过。");
    }

    private void verifyDolphinPlugin() {
        // pg_extension 能直接证明当前 openGauss 实例安装了 dolphin 插件；
        // pg_settings 能证明服务端已经配置 MySQL 协议开关和 dolphin 监听端口。
        //noinspection SqlResolve
        Integer extensionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pg_extension WHERE extname = ?",
                Integer.class,
                "dolphin");
        System.out.println("dolphin 插件安装数量：" + extensionCount);
        if (extensionCount == null || extensionCount < 1) {
            throw new IllegalStateException("dolphin 插件校验失败：pg_extension 中没有查到 dolphin。");
        }

        String enableDolphinProto = queryPgSetting("enable_dolphin_proto");
        String dolphinServerPort = queryPgSetting("dolphin_server_port");
        System.out.println("enable_dolphin_proto 配置值：" + enableDolphinProto);
        System.out.println("dolphin_server_port 配置值：" + dolphinServerPort);
        if (!"on".equalsIgnoreCase(enableDolphinProto)) {
            throw new IllegalStateException("dolphin MySQL 协议开关校验失败：enable_dolphin_proto=" + enableDolphinProto);
        }
        if (!"3306".equals(dolphinServerPort)) {
            throw new IllegalStateException("dolphin MySQL 协议端口校验失败：dolphin_server_port=" + dolphinServerPort);
        }

        System.out.println("dolphin 插件与 MySQL 协议配置校验通过。");
    }

    private String queryPgSetting(String name) {
        //noinspection SqlResolve
        List<String> values = jdbcTemplate.query("SELECT setting FROM pg_settings WHERE name = ?",
                (rs, rowNum) -> rs.getString("setting"),
                name);
        if (values.isEmpty()) {
            throw new IllegalStateException("配置项校验失败：pg_settings 中没有查到 " + name + "。");
        }
        return values.get(0);
    }

    private void verifySchemaMapping() {
        // dolphin MySQL 协议下，客户端 URL 中的 database 名称会影响当前 schema。
        // 这里把 DATABASE() 和 current_schema() 同时打印出来，避免文档把数据库和 schema 映射关系写模糊。
        //noinspection SqlResolve
        jdbcTemplate.query("SELECT DATABASE() AS client_database, current_schema() AS current_schema_name", rs -> {
            String clientDatabase = rs.getString("client_database");
            String currentSchema = rs.getString("current_schema_name");
            System.out.println("JDBC URL 客户端库名 DATABASE()：" + clientDatabase);
            System.out.println("openGauss 当前 schema current_schema()：" + currentSchema);
            if (!"mysql_test_db".equals(clientDatabase)) {
                throw new IllegalStateException("客户端库名映射异常：预期 DATABASE() 为 mysql_test_db，实际为 " + clientDatabase);
            }
            if (!"mysql_test_db".equals(currentSchema)) {
                throw new IllegalStateException("当前 schema 映射异常：预期 current_schema() 为 mysql_test_db，实际为 " + currentSchema);
            }
        });
        System.out.println("dolphin 库名与 schema 映射校验通过。");
    }

    private void cleanupPreviousRunRows() {
        // 只删除本验证程序生成的临时数据，不影响文档准备阶段插入的 3 条基础数据。
        int fixedRows = jdbcTemplate.update("DELETE FROM " + TABLE_NAME + " WHERE name IN (?, ?, ?, ?)",
                "zhaoliu", "zhaoliuliuliu", "spring-transaction", "pool-limit-user");
        int concurrentRows = jdbcTemplate.update("DELETE FROM " + TABLE_NAME + " WHERE name LIKE ?",
                "concurrent-user-%");
        System.out.println("历史临时数据清理结果：固定名称数据 " + fixedRows + " 行，并发名称数据 " + concurrentRows + " 行。");
    }

    private void insertUser(String name, int age) {
        int rows = jdbcTemplate.update("INSERT INTO " + TABLE_NAME + " (name, age) VALUES (?, ?)", name, age);
        System.out.println("INSERT 执行影响行数：" + rows);
        if (rows != 1) {
            throw new IllegalStateException("INSERT 校验失败：预期影响 1 行，实际影响 " + rows + " 行。");
        }
    }

    private void updateUser(int id, String name, int age) {
        int rows = jdbcTemplate.update("UPDATE " + TABLE_NAME + " SET name = ?, age = ? WHERE id = ?", name, age, id);
        System.out.println("UPDATE 执行影响行数：" + rows);
        if (rows != 1) {
            throw new IllegalStateException("UPDATE 校验失败：预期影响 1 行，实际影响 " + rows + " 行。");
        }
    }

    private void deleteUser(int id) {
        int rows = jdbcTemplate.update("DELETE FROM " + TABLE_NAME + " WHERE id = ?", id);
        System.out.println("DELETE 执行影响行数：" + rows);
        if (rows != 1) {
            throw new IllegalStateException("DELETE 校验失败：预期影响 1 行，实际影响 " + rows + " 行。");
        }
    }

    private Integer getUserIdByName(String name) {
        List<Integer> ids = jdbcTemplate.query("SELECT id FROM " + TABLE_NAME + " WHERE name = ? ORDER BY id DESC",
                (rs, rowNum) -> rs.getInt("id"),
                name);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private void verifyInitialUsers() {
        List<User> users = queryUsers();
        printUsers("初始查询", users);

        Set<String> existingNames = new HashSet<>();
        for (User user : users) {
            existingNames.add(user.getName());
        }
        for (String requiredName : REQUIRED_INITIAL_USERS) {
            if (!existingNames.contains(requiredName)) {
                throw new IllegalStateException("初始业务表校验失败：没有查到文档准备数据 " + requiredName + "。");
            }
        }
        System.out.println("初始业务表校验通过：已查到张三、李四、王五三条基础数据。");
    }

    private void selectUsers(String label) {
        List<User> users = queryUsers();
        printUsers(label, users);
        if (users.size() < REQUIRED_INITIAL_USERS.size()) {
            throw new IllegalStateException("查询结果校验失败：预期至少 "
                    + REQUIRED_INITIAL_USERS.size() + " 条基础数据，实际只有 " + users.size() + " 条。");
        }
        System.out.println(label + "查询校验通过：基础数据仍然存在。");
    }

    private List<User> queryUsers() {
        return jdbcTemplate.query("SELECT id, name, age FROM " + TABLE_NAME + " ORDER BY id",
                (rs, rowNum) -> new User(rs.getInt("id"), rs.getString("name"), rs.getInt("age")));
    }

    private void printUsers(String label, List<User> users) {
        System.out.println(label + "行数：" + users.size());
        for (User user : users) {
            System.out.println("  用户记录：id=" + user.getId()
                    + "，name=" + user.getName()
                    + "，age=" + user.getAge());
        }
    }

    private void assertUserExists(String name, int age) {
        Integer rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE name = ? AND age = ?",
                Integer.class,
                name,
                age);
        if (rows == null || rows != 1) {
            throw new IllegalStateException("用户数据校验失败：预期存在 name=" + name + " 且 age=" + age + " 的 1 行数据，实际为 " + rows + " 行。");
        }
        System.out.println("用户数据校验通过：name=" + name + "，age=" + age + "。");
    }

    private void verifyTransaction() {
        // 使用 Spring 的 TransactionTemplate，验证 Spring 事务管理器能基于 Hikari 连接正常提交事务。
        transactionTemplate.executeWithoutResult(status ->
                jdbcTemplate.update("INSERT INTO " + TABLE_NAME + " (name, age) VALUES (?, ?)", "spring-transaction", 22));

        Integer rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE name = ?",
                Integer.class,
                "spring-transaction");
        if (rows == null || rows != 1) {
            throw new IllegalStateException("事务提交校验失败：预期 spring-transaction 为 1 行，实际为 " + rows + " 行。");
        }

        System.out.println("事务提交校验通过：spring-transaction 数据已提交并可查询。");
    }

    private void verifyPoolLimit(HikariDataSource hikariDataSource) throws Exception {
        int maxPoolSize = hikariDataSource.getMaximumPoolSize();
        long connectionTimeoutMs = hikariDataSource.getConnectionTimeout();
        // 观察窗口取 connectionTimeout 的一半并夹在 200ms 到 1000ms 之间，
        // 避免配置较小的 connectionTimeout 时，连接池按配置正常超时却被误判为“未进入等待队列”。
        long observeWindowMs = Math.max(200L, Math.min(1000L, connectionTimeoutMs / 2));
        List<Connection> borrowedConnections = new ArrayList<>();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<Boolean> waitingConnection = null;

        try {
            // 先把连接池借满，模拟业务请求已经占满全部连接的场景。
            for (int i = 0; i < maxPoolSize; i++) {
                borrowedConnections.add(dataSource.getConnection());
            }

            HikariPoolMXBean poolMxBean = hikariDataSource.getHikariPoolMXBean();
            if (poolMxBean != null) {
                System.out.println("连接池借满后活跃连接数：" + poolMxBean.getActiveConnections());
                System.out.println("连接池借满后空闲连接数：" + poolMxBean.getIdleConnections());
                System.out.println("连接池借满后等待线程数：" + poolMxBean.getThreadsAwaitingConnection());
            }
            System.out.println("已借出连接数：" + borrowedConnections.size() + "，配置最大连接数：" + maxPoolSize);
            System.out.println("第 " + (maxPoolSize + 1) + " 条连接观察窗口：" + observeWindowMs
                    + " ms，连接池 connectionTimeout=" + connectionTimeoutMs + " ms");

            waitingConnection = executorService.submit(() -> {
                try (Connection connection = dataSource.getConnection()) {
                    return connection.isValid(5);
                }
            });

            Thread.sleep(observeWindowMs);
            if (waitingConnection.isDone()) {
                // isDone() 在“正常返回”与“异常结束”时均为 true，必须进一步区分，
                // 否则 CannotGetJdbcConnectionException 等异常结束会被误判为“违规放行”。
                // 任务已结束，直接使用不带超时的 get() 取结果，避免产生不可达的超时分支。
                try {
                    waitingConnection.get();
                    // 任务正常返回：第 (maxPoolSize+1) 条连接被立即获取成功 => 校验失败
                    throw new IllegalStateException("连接池上限校验失败：第 " + (maxPoolSize + 1)
                            + " 条连接在连接池已满时立即获取成功，说明 maximumPoolSize 没有形成有效限制。");
                } catch (ExecutionException ee) {
                    Throwable cause = ee.getCause();
                    if (cause instanceof SQLTransientConnectionException) {
                        // 请求在等待队列中按 connectionTimeout 超时，说明连接池没有突破上限。
                        // 该请求已经结束，不再继续验证“释放连接后恢复”。
                        System.out.println("连接池上限等待校验通过：第 " + (maxPoolSize + 1)
                                + " 条连接未突破最大连接数，等待 " + connectionTimeoutMs
                                + " ms 后按 connectionTimeout 超时。");
                        return;
                    }
                    throw new IllegalStateException("连接池上限校验未达预期：第 " + (maxPoolSize + 1)
                            + " 条连接请求以非等待超时异常结束（" + cause.getClass().getSimpleName() + "）。", cause);
                }
            }

            if (poolMxBean != null) {
                System.out.println("申请第 " + (maxPoolSize + 1) + " 条连接时等待线程数：" + poolMxBean.getThreadsAwaitingConnection());
            }

            closeOneBorrowedConnection(borrowedConnections);
            Boolean connectionValid = waitingConnection.get(10, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(connectionValid)) {
                throw new IllegalStateException("释放连接后，第 " + (maxPoolSize + 1) + " 条连接获取成功但有效性校验失败。");
            }

            System.out.println("连接池上限等待校验通过：第 " + (maxPoolSize + 1) + " 条连接没有突破最大连接数，而是在等待空闲连接。");

            jdbcTemplate.update("INSERT INTO " + TABLE_NAME + " (name, age) VALUES (?, ?)", "pool-limit-user", 33);
            assertUserExists("pool-limit-user", 33);
            System.out.println("连接池上限恢复校验通过：释放 1 条连接后，等待中的请求可以继续获取连接并执行 SQL。");
        } catch (TimeoutException e) {
            throw new IllegalStateException("连接池上限校验失败：释放连接后，等待中的请求仍未在 10 秒内获取连接。", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("连接池上限校验失败：等待中的连接请求执行异常。", e.getCause());
        } finally {
            if (waitingConnection != null && !waitingConnection.isDone()) {
                waitingConnection.cancel(true);
            }
            for (Connection connection : borrowedConnections) {
                closeQuietly(connection);
            }
            executorService.shutdownNow();
        }
    }

    private void closeOneBorrowedConnection(List<Connection> borrowedConnections) {
        if (borrowedConnections.isEmpty()) {
            throw new IllegalStateException("连接池上限校验失败：没有可释放的已借出连接。");
        }

        Connection connection = borrowedConnections.remove(borrowedConnections.size() - 1);
        closeQuietly(connection);
        System.out.println("已释放 1 条占用连接，用于确认等待中的连接请求可以恢复执行。");
    }

    private void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (Exception e) {
            System.out.println("关闭连接时出现异常，继续执行清理：" + e.getMessage());
        }
    }

    private void verifyConcurrentOperations() throws Exception {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        // ready 确保所有 worker 已创建完毕；start 让所有 worker 同时发起连接获取。
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        // connectionsAcquired 确保 10 个 worker 都已拿到连接后，再进入真实 SQL 操作。
        // 这样验证的是并发连接上的并发数据库操作，而不是串行借还同一个连接。
        CountDownLatch connectionsAcquired = new CountDownLatch(threadCount);
        AtomicInteger passed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                futures.add(executorService.submit(() -> {
                    String originalName = "concurrent-user-" + index + "-" + System.nanoTime();
                    String updatedName = originalName + "-updated";
                    ready.countDown();
                    try {
                        start.await();
                        try (Connection connection = dataSource.getConnection()) {
                            connectionsAcquired.countDown();
                            if (!connectionsAcquired.await(10, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("并发连接校验失败：不是所有 worker 都能在 10 秒内拿到连接。");
                            }

                            connection.setAutoCommit(false);
                            try {
                                // 每个 worker 使用不同 name，避免并发验证之间互相抢同一行。
                                int id = insertConcurrentUser(connection, originalName, 20 + index);
                                assertConcurrentName(connection, id, originalName);
                                updateConcurrentUser(connection, id, updatedName, 30 + index);
                                assertConcurrentName(connection, id, updatedName);
                                deleteConcurrentUser(connection, id);
                                connection.commit();
                                passed.incrementAndGet();
                            } catch (Exception e) {
                                connection.rollback();
                                throw e;
                            } finally {
                                connection.setAutoCommit(true);
                            }
                        }
                    } catch (Exception e) {
                        failed.incrementAndGet();
                        throw new IllegalStateException("第 " + index + " 个并发 worker 执行数据库操作失败。", e);
                    }
                }));
            }

                try {
                    if (!ready.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("并发 worker 启动超时：10 秒内没有全部准备完成。");
                    }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    throw new IllegalStateException("并发 worker 启动被中断。", e);
                } finally {
                    if (!ready.await(0, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                    }
                }

            start.countDown();
            try {
    for (Future<?> future : futures) {
        try {
            future.get(30, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw new IllegalStateException("并发数据库操作校验失败：成功 " + passed.get()
                    + " 个，失败 " + failed.get() + " 个。", e.getCause());
        }
    }

    // 所有 worker 正常完成，passed 应等于 threadCount
    // 无需额外检查，直接打印通过信息


    System.out.println("并发真实数据库操作校验通过：" + passed.get()
            + "/" + threadCount + " 个 worker 均完成 insert/select/update/select/delete/commit。");
} catch (TimeoutException e) {
    throw new IllegalStateException("并发数据库操作校验超时：存在 worker 在 30 秒内没有完成真实 SQL 操作。", e);
} finally {
            executorService.shutdownNow();
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                System.out.println("并发线程池未能在 10 秒内完全停止，程序继续退出。");
            }
        }
    }

    private int insertConcurrentUser(Connection connection, String name, int age) throws Exception {
        // 先尝试 JDBC generated keys；如果 dolphin/驱动组合没有返回 generated key，
        // 再按唯一 name 查询 id，保证验证能覆盖更多兼容实现。
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO " + TABLE_NAME + " (name, age) VALUES (?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setInt(2, age);
            int rows = statement.executeUpdate();
            if (rows != 1) {
                throw new IllegalStateException("并发 INSERT 校验失败：预期影响 1 行，实际影响 " + rows + " 行。");
            }

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM " + TABLE_NAME + " WHERE name = ?")) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        throw new IllegalStateException("并发 INSERT 后没有查到新数据 id，name=" + name);
    }

    private void updateConcurrentUser(Connection connection, int id, String name, int age) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE " + TABLE_NAME + " SET name = ?, age = ? WHERE id = ?")) {
            statement.setString(1, name);
            statement.setInt(2, age);
            statement.setInt(3, id);
            int rows = statement.executeUpdate();
            if (rows != 1) {
                throw new IllegalStateException("并发 UPDATE 校验失败：预期影响 1 行，实际影响 " + rows + " 行。");
            }
        }
    }

    private void assertConcurrentName(Connection connection, int id, String expectedName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name FROM " + TABLE_NAME + " WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("并发 SELECT 校验失败：没有查到 id=" + id + " 的数据。");
                }
                String actualName = rs.getString("name");
                if (!expectedName.equals(actualName)) {
                    throw new IllegalStateException("并发 SELECT 校验失败：预期 name=" + expectedName + "，实际 name=" + actualName + "。");
                }
            }
        }
    }

    private void deleteConcurrentUser(Connection connection, int id) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM " + TABLE_NAME + " WHERE id = ?")) {
            statement.setInt(1, id);
            int rows = statement.executeUpdate();
            if (rows != 1) {
                throw new IllegalStateException("并发 DELETE 校验失败：预期影响 1 行，实际影响 " + rows + " 行。");
            }
        }
    }
}
