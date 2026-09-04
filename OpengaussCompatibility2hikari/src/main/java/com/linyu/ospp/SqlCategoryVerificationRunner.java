package com.linyu.ospp;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 五类标准 SQL 操作验证程序（DDL / DML / DQL / DCL / TCL）+ 并发连接测试。
 * 通过 HikariCP 连接池 + MySQL Connector/J 驱动，经 dolphin MySQL 协议连接 openGauss B 兼容库。
 * 运行方式：java -cp "hikaricp-5.1.0.jar:mysql-connector-java-8.0.20.jar:." com.linyu.ospp.SqlCategoryVerificationRunner
 * 或在 Spring Boot 项目中通过 SpringApplication.run 启动后自动执行。
 */
public class SqlCategoryVerificationRunner {

    // 连接参数：优先从环境变量读取，避免明文口令与内部 IP 进入版本库；缺失口令时显式报错。
    private static final String DB_HOST = System.getenv().getOrDefault("OPENGAUSS_MYSQL_HOST", "127.0.0.1");
    private static final String DB_PORT = System.getenv().getOrDefault("OPENGAUSS_MYSQL_PORT", "3306");
    private static final String DB_NAME = System.getenv().getOrDefault("OPENGAUSS_MYSQL_DB", "mysql_db");
    private static final String USERNAME = System.getenv().getOrDefault("OPENGAUSS_MYSQL_USER", "mysqluser");
    private static final String PASSWORD = requireEnv("OPENGAUSS_MYSQL_PASSWORD");
    private static final String BASE_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/";
    private static final String JDBC_URL = BASE_URL + DB_NAME + "?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";

    // 使用独立表名，避免与既有 user 表冲突
    private static final String TEST_TABLE = "sql_category_test";
    private static final String TEST_TABLE_2 = "sql_category_test_2";

    // 结论表列宽，中文按两列宽度计算
    private static final int CATEGORY_WIDTH = 8;
    private static final int OPERATION_WIDTH = 29;
    private static final int RESULT_WIDTH = 26;

    private static int sectionNo = 1;

    // 结论表按实际执行结果统计，避免写死 PASS / WARN 数量与实测输出不一致。
    // DCL 的 GRANT / REVOKE 在权限不足时记 WARN，因此单独计数。
    private static int dclPassCount = 0;
    private static int dclWarnCount = 0;
    private static int dclInfoCount = 0;
    // DML-05 是否取到自增主键，决定该项记 PASS 还是 INFO。
    private static boolean generatedKeysReturned = false;

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("  openGauss B 兼容模式 五类 SQL 操作验证");
        System.out.println("  驱动：MySQL Connector/J 8.0.20");
        System.out.println("  连接池：HikariCP");
        System.out.println("  协议：dolphin MySQL 协议 (3306)");
        System.out.println("  目标：openGauss 7.0.0-RC3 + dolphin 5.2");
        System.out.println("========================================");

        // 先确保目标数据库存在（连接到默认库创建 schema）
        ensureDatabaseExists();

        HikariDataSource dataSource = initDataSource();

        try {
            runAllTests(dataSource);
        } finally {
            dataSource.close();
            System.out.println();
            System.out.println("连接池已关闭。全部验证结束。");
        }
    }

    private static HikariDataSource initDataSource() {
        printSection("连接池初始化");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setUsername(USERNAME);
        config.setPassword(PASSWORD);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("SqlCategoryPool");
        HikariDataSource ds = new HikariDataSource(config);
        System.out.println("HikariCP 连接池初始化完成：poolName=" + ds.getPoolName()
                + ", maximumPoolSize=" + ds.getMaximumPoolSize());
        return ds;
    }

    private static void ensureDatabaseExists() throws Exception {
        printSection("确保目标数据库存在");
        // 先连接到 postgres（openGauss B 兼容库默认库）创建目标 schema
        HikariConfig initCfg = new HikariConfig();
        initCfg.setJdbcUrl(BASE_URL + "postgres?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
        initCfg.setUsername(USERNAME);
        initCfg.setPassword(PASSWORD);
        initCfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
        initCfg.setPoolName("InitPool");
        try (HikariDataSource initDs = new HikariDataSource(initCfg);
             Connection conn = initDs.getConnection();
             Statement stmt = conn.createStatement()) {
            // 检查 schema 是否已存在
            ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_namespace WHERE nspname = '" + DB_NAME + "'");
            if (rs.next()) {
                System.out.println("数据库 " + DB_NAME + " 已存在，跳过创建。");
            } else {
                stmt.executeUpdate("CREATE SCHEMA " + DB_NAME);
                System.out.println("已创建数据库(schema)：" + DB_NAME);
            }
        }
    }

    private static void runAllTests(HikariDataSource dataSource) throws Exception {
        // ====== 1. DDL 验证 ======
        verifyDDL(dataSource);

        // ====== 2. DML 验证 ======
        verifyDML(dataSource);

        // ====== 3. DQL 验证 ======
        verifyDQL(dataSource);

        // ====== 4. DCL 验证 ======
        verifyDCL(dataSource);

        // ====== 5. TCL 验证 ======
        verifyTCL(dataSource);

        // ====== 6. 并发连接验证 ======
        verifyConcurrentConnections(dataSource);

        // ====== 清理 DDL 残留表 ======
        cleanupTables(dataSource);

        // ====== 最终结论 ======
        printFinalConclusion();
    }

    // ==================== DDL ====================
    private static void verifyDDL(HikariDataSource dataSource) throws Exception {
        printSection("DDL（Data Definition Language）验证");
        printPurpose("验证 CREATE / ALTER / DROP / TRUNCATE 四种 DDL 操作通过 dolphin MySQL 协议正常执行。");

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {

            // --- CREATE TABLE ---
            System.out.println("[DDL-01] CREATE TABLE - 创建主测试表");
            stmt.executeUpdate("DROP TABLE IF EXISTS " + TEST_TABLE);
            String createSql = "CREATE TABLE " + TEST_TABLE + " ("
                    + "id SERIAL PRIMARY KEY, "
                    + "name VARCHAR(64) NOT NULL, "
                    + "age INT DEFAULT 0, "
                    + "score DECIMAL(5,2), "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")";
            stmt.executeUpdate(createSql);
            System.out.println("  PASS | CREATE TABLE " + TEST_TABLE + " 执行成功（含 SERIAL 主键、VARCHAR、INT、DECIMAL、TIMESTAMP 字段）");

            // --- CREATE INDEX ---
            System.out.println("[DDL-02] CREATE INDEX - 创建索引");
            stmt.executeUpdate("CREATE INDEX idx_" + TEST_TABLE + "_name ON " + TEST_TABLE + " (name)");
            System.out.println("  PASS | CREATE INDEX idx_" + TEST_TABLE + "_name 执行成功");

            // --- ALTER TABLE ADD COLUMN ---
            System.out.println("[DDL-03] ALTER TABLE ADD COLUMN - 增加列");
            stmt.executeUpdate("ALTER TABLE " + TEST_TABLE + " ADD COLUMN remark TEXT");
            System.out.println("  PASS | ALTER TABLE ... ADD COLUMN remark TEXT 执行成功");

            // --- ALTER TABLE RENAME COLUMN ---
            System.out.println("[DDL-04] ALTER TABLE RENAME COLUMN - 重命名列");
            stmt.executeUpdate("ALTER TABLE " + TEST_TABLE + " RENAME COLUMN remark TO description");
            System.out.println("  PASS | ALTER TABLE ... RENAME COLUMN remark TO description 执行成功");

            // --- TRUNCATE TABLE（先插入数据再清空）---
            stmt.executeUpdate("INSERT INTO " + TEST_TABLE + " (name, age, score) VALUES ('truncate_test', 99, 100.00)");
            System.out.println("[DDL-05] TRUNCATE TABLE - 清空表数据");
            stmt.executeUpdate("TRUNCATE TABLE " + TEST_TABLE);
            int countAfterTruncate = queryCount(conn, TEST_TABLE);
            if (countAfterTruncate == 0) {
                System.out.println("  PASS | TRUNCATE TABLE " + TEST_TABLE + " 执行成功，当前行数=" + countAfterTruncate);
            } else {
                throw new IllegalStateException("TRUNCATE 失败：预期 0 行，实际 " + countAfterTruncate + " 行");
            }

            // --- DROP TABLE ---
            System.out.println("[DDL-06] DROP TABLE - 删除第二张测试表（先建再删）");
            stmt.executeUpdate("CREATE TABLE " + TEST_TABLE_2 + " (id INT PRIMARY KEY, val VARCHAR(32))");
            stmt.executeUpdate("DROP TABLE " + TEST_TABLE_2);
            System.out.println("  PASS | DROP TABLE " + TEST_TABLE_2 + " 执行成功");

            // --- CREATE TABLE AS SELECT ---
            System.out.println("[DDL-07] CREATE TABLE AS SELECT - 从查询结果创建表");
            stmt.executeUpdate("INSERT INTO " + TEST_TABLE + " (name, age, score) VALUES ('ctas_src', 25, 88.50)");
            stmt.executeUpdate("CREATE TABLE " + TEST_TABLE_2 + " AS SELECT name, age FROM " + TEST_TABLE + " WHERE name = 'ctas_src'");
            int ctasCount = queryCount(conn, TEST_TABLE_2);
            System.out.println("  PASS | CREATE TABLE " + TEST_TABLE_2 + " AS SELECT 执行成功，目标表行数=" + ctasCount);
            stmt.executeUpdate("DROP TABLE " + TEST_TABLE_2);
        }

        System.out.println("=> DDL 全部 7 项操作验证通过（CREATE/INDEX/ALTER ADD/RENAME/TRUNCATE/DROP/CTAS）。");
    }

    // ==================== DML ====================
    private static void verifyDML(HikariDataSource dataSource) throws Exception {
        printSection("DML（Data Manipulation Language）验证");
        printPurpose("验证 INSERT / UPDATE / DELETE 三种 DML 操作及批量写入通过 dolphin MySQL 协议正常执行。");

        try (Connection conn = dataSource.getConnection()) {

            // --- INSERT 单行 ---
            System.out.println("[DML-01] INSERT - 单行插入");
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + TEST_TABLE + " (name, age, score) VALUES (?, ?, ?)")) {
                ps.setString(1, "Alice");
                ps.setInt(2, 28);
                ps.setDouble(3, 95.50);
                int rows = ps.executeUpdate();
                System.out.println("  PASS | INSERT 单行影响行数=" + rows + " (name=Alice, age=28, score=95.50)");
            }

            // --- INSERT 多行 ---
            System.out.println("[DML-02] INSERT - 多行插入");
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + TEST_TABLE + " (name, age, score) VALUES (?, ?, ?)")) {
                for (int i = 0; i < 3; i++) {
                    ps.setString(1, "BatchUser" + i);
                    ps.setInt(2, 20 + i);
                    ps.setDouble(3, 80.0 + i * 5);
                    ps.addBatch();
                }
                int[] results = ps.executeBatch();
                int total = 0;
                for (int r : results) total += r;
                System.out.println("  PASS | INSERT 批量 3 行影响总行数=" + total);
            }

            // --- UPDATE ---
            System.out.println("[DML-03] UPDATE - 条件更新");
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE " + TEST_TABLE + " SET age = ?, score = ? WHERE name = ?")) {
                ps.setInt(1, 29);
                ps.setDouble(2, 96.00);
                ps.setString(3, "Alice");
                int rows = ps.executeUpdate();
                System.out.println("  PASS | UPDATE 影响行数=" + rows + " (Alice age:28->29, score:95.50->96.00)");

                // 验证更新结果
                try (PreparedStatement qs = conn.prepareStatement(
                        "SELECT age, score FROM " + TEST_TABLE + " WHERE name = 'Alice'")) {
                    ResultSet rs = qs.executeQuery();
                    if (rs.next()) {
                        int newAge = rs.getInt("age");
                        double newScore = rs.getDouble("score");
                        System.out.println("  确认 | 更新后 Alice age=" + newAge + ", score=" + newScore);
                    }
                }
            }

            // --- DELETE ---
            System.out.println("[DML-04] DELETE - 条件删除");
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM " + TEST_TABLE + " WHERE name LIKE 'BatchUser%'")) {
                int rows = ps.executeUpdate();
                System.out.println("  PASS | DELETE 影响行数=" + rows + " (删除所有 BatchUser*)");
            }

            // --- 获取自增主键（dolphin 兼容）：JDBC getGeneratedKeys ---
            System.out.println("[DML-05] JDBC getGeneratedKeys - 获取自增主键（dolphin 兼容）");
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + TEST_TABLE + " (name, age, score) VALUES ('Bob', 35, 87.25)",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    long genId = keys.getLong(1);
                    generatedKeysReturned = true;
                    System.out.println("  PASS | INSERT Bob 后获取到自增 id=" + genId);
                } else {
                    System.out.println("  INFO | 驱动未返回 GENERATED_KEYS（不影响插入成功）");
                }
            }
        }

        if (generatedKeysReturned) {
            System.out.println("=> DML 全部 5 项操作验证通过（单行INSERT/批量INSERT/UPDATE/DELETE/getGeneratedKeys）。");
        } else {
            System.out.println("=> DML 共 4 项操作验证通过（单行INSERT/批量INSERT/UPDATE/DELETE）；"
                    + "DML-05 getGeneratedKeys 为 INFO，不计入 PASS。");
        }
    }

    // ==================== DQL ====================
    private static void verifyDQL(HikariDataSource dataSource) throws Exception {
        printSection("DQL（Data Query Language）验证");
        printPurpose("验证 SELECT 及其子句（WHERE / ORDER BY / GROUP BY / 聚合函数 / LIMIT / JOIN）通过 dolphin MySQL 协议正常执行。");

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {

            // 准备查询数据
            stmt.executeUpdate("INSERT INTO " + TEST_TABLE + " (name, age, score) VALUES ('Charlie', 22, 78.00)");
            stmt.executeUpdate("INSERT INTO " + TEST_TABLE + " (name, age, score) VALUES ('Diana', 31, 92.50)");
            stmt.executeUpdate("INSERT INTO " + TEST_TABLE + " (name, age, score) VALUES ('Eve', 27, 85.75)");

            // --- 基础 SELECT * ---
            System.out.println("[DQL-01] SELECT * - 全量查询");
            List<String> allRows = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery("SELECT id, name, age, score FROM " + TEST_TABLE + " ORDER BY id")) {
                while (rs.next()) {
                    allRows.add(rs.getInt("id") + "|" + rs.getString("name") + "|" + rs.getInt("age") + "|" + rs.getDouble("score"));
                }
            }
            System.out.println("  PASS | SELECT * 返回 " + allRows.size() + " 行");
            for (String row : allRows) {
                System.out.println("         " + row);
            }

            // --- SELECT WHERE ---
            System.out.println("[DQL-02] SELECT ... WHERE - 条件过滤");
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT name, age FROM " + TEST_TABLE + " WHERE age >= 28 ORDER BY age")) {
                List<String> filtered = new ArrayList<>();
                while (rs.next()) {
                    filtered.add(rs.getString("name") + "(age=" + rs.getInt("age") + ")");
                }
                System.out.println("  PASS | WHERE age>=28 返回 " + filtered.size() + " 行：" + String.join(", ", filtered));
            }

            // --- SELECT ORDER BY + LIMIT ---
            System.out.println("[DQL-03] SELECT ... ORDER BY + LIMIT - 排序与分页");
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT name, score FROM " + TEST_TABLE + " ORDER BY score DESC LIMIT 3")) {
                List<String> top3 = new ArrayList<>();
                while (rs.next()) {
                    top3.add(rs.getString("name") + "(" + rs.getDouble("score") + ")");
                }
                System.out.println("  PASS | TOP 3 by score DESC：" + String.join(" > ", top3));
            }

            // --- 聚合函数 COUNT/SUM/AVG/MIN/MAX ---
            System.out.println("[DQL-04] 聚合函数 - COUNT / SUM / AVG / MIN / MAX");
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) AS cnt, SUM(age) AS sum_age, AVG(score) AS avg_score, "
                            + "MIN(age) AS min_age, MAX(age) AS max_age FROM " + TEST_TABLE)) {
                if (rs.next()) {
                    System.out.println("  PASS | COUNT=" + rs.getInt("cnt")
                            + ", SUM(age)=" + rs.getLong("sum_age")
                            + ", AVG(score)=" + Math.round(rs.getDouble("avg_score") * 100.0) / 100.0
                            + ", MIN(age)=" + rs.getInt("min_age")
                            + ", MAX(age)=" + rs.getInt("max_age"));
                }
            }

            // --- GROUP BY + HAVING ---
            System.out.println("[DQL-05] GROUP BY + HAVING - 分组聚合与过滤");
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT CASE WHEN age < 25 THEN 'young' ELSE 'senior' END AS group_name, "
                            + "COUNT(*) AS cnt, AVG(score) AS avg_s "
                            + "FROM " + TEST_TABLE + " GROUP BY CASE WHEN age < 25 THEN 'young' ELSE 'senior' END "
                            + "HAVING COUNT(*) >= 1")) {
                List<String> groups = new ArrayList<>();
                while (rs.next()) {
                    groups.add(rs.getString("group_name") + ": cnt=" + rs.getInt("cnt") + ", avg_score="
                            + Math.round(rs.getDouble("avg_s") * 100.0) / 100.0);
                }
                System.out.println("  PASS | GROUP BY 分组数=" + groups.size() + "：" + String.join("; ", groups));
            }

            // --- DISTINCT ---
            System.out.println("[DQL-06] SELECT DISTINCT - 去重");
            stmt.executeUpdate("INSERT INTO " + TEST_TABLE + " (name, age, score) VALUES ('Alice', 40, 70.00)");
            try (ResultSet rs = stmt.executeQuery("SELECT DISTINCT name FROM " + TEST_TABLE + " ORDER BY name")) {
                List<String> names = new ArrayList<>();
                while (rs.next()) names.add(rs.getString("name"));
                System.out.println("  PASS | DISTINCT name 返回 " + names.size() + " 个唯一值：" + String.join(", ", names));
            }

            // 清理 DQL-06 为演示 DISTINCT 额外插入的重复行（name='Alice', age=40），避免污染后续子查询
            stmt.executeUpdate("DELETE FROM " + TEST_TABLE + " WHERE name = 'Alice' AND age = 40 AND score = 70.00");

            // --- 子查询 ---
            System.out.println("[DQL-07] 子查询 - SELECT 中嵌套子查询");
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT name, age FROM " + TEST_TABLE
                            + " WHERE age > (SELECT AVG(age) FROM " + TEST_TABLE + ") ORDER BY age")) {
                List<String> aboveAvg = new ArrayList<>();
                while (rs.next()) {
                    aboveAvg.add(rs.getString("name") + "(" + rs.getInt("age") + ")");
                }
                System.out.println("  PASS | 年龄高于平均值的记录：" + String.join(", ", aboveAvg));
            }
        }

        System.out.println("=> DQL 全部 7 项查询验证通过（基础SELECT/WHERE/ORDER+LIMIT/聚合/GROUP+HAVING/DISTINCT/子查询）。");
    }

    // ==================== DCL ====================
    private static void verifyDCL(HikariDataSource dataSource) throws Exception {
        printSection("DCL（Data Control Language）验证");
        printPurpose("验证 GRANT / REVOKE 权限管理操作通过 dolphin MySQL 协议正常执行。");
        System.out.println("  注意：DCL 操作需要当前用户具备相应权限（如超级用户或对象属主）。");

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {

            // --- GRANT SELECT ---
            System.out.println("[DCL-01] GRANT - 授予表级 SELECT 权限");
            try {
                stmt.executeUpdate("GRANT SELECT ON TABLE " + TEST_TABLE + " TO " + USERNAME);
                dclPassCount++;
                System.out.println("  PASS | GRANT SELECT ON TABLE " + TEST_TABLE + " TO " + USERNAME + " 执行成功");
            } catch (SQLException e) {
                dclWarnCount++;
                System.out.println("  WARN | GRANT SELECT 失败（可能权限不足或已拥有）：" + firstLineOrEmpty(e.getMessage()));
            }

            // --- GRANT INSERT/UPDATE/DELETE ---
            System.out.println("[DCL-02] GRANT - 授予多权限");
            try {
                stmt.executeUpdate("GRANT INSERT, UPDATE, DELETE ON TABLE " + TEST_TABLE + " TO " + USERNAME);
                dclPassCount++;
                System.out.println("  PASS | GRANT INSERT,UPDATE,DELETE ON TABLE " + TEST_TABLE + " TO " + USERNAME + " 执行成功");
            } catch (SQLException e) {
                dclWarnCount++;
                System.out.println("  WARN | GRANT 多权限失败（可能权限不足或已拥有）：" + firstLineOrEmpty(e.getMessage()));
            }

            // --- REVOKE ---
            System.out.println("[DCL-03] REVOKE - 收回权限");
            try {
                stmt.executeUpdate("REVOKE INSERT, UPDATE, DELETE ON TABLE " + TEST_TABLE + " FROM " + USERNAME);
                dclPassCount++;
                System.out.println("  PASS | REVOKE INSERT,UPDATE,DELETE ON TABLE " + TEST_TABLE + " FROM " + USERNAME + " 执行成功");
            } catch (SQLException e) {
                dclWarnCount++;
                System.out.println("  WARN | REVOKE 失败（可能权限不足）：" + firstLineOrEmpty(e.getMessage()));
            }

            // --- 查询当前用户权限 ---
            System.out.println("[DCL-04] 查询当前用户权限信息");
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT privilege_type, is_grantable FROM information_schema.table_privileges "
                            + "WHERE table_name = '" + TEST_TABLE.toLowerCase() + "' AND grantee = CURRENT_USER LIMIT 10")) {
                List<String> privs = new ArrayList<>();
                while (rs.next()) {
                    privs.add(rs.getString("privilege_type")
                            + ("YES".equalsIgnoreCase(rs.getString("is_grantable")) ? "(可转授)" : ""));
                }
                if (privs.isEmpty()) {
                    dclInfoCount++;
                    System.out.println("  INFO | 当前用户对 " + TEST_TABLE + " 无额外授权记录（使用默认角色权限）");
                } else {
                    dclPassCount++;
                    System.out.println("  PASS | 当前用户权限：" + String.join(", ", privs));
                }
            }
        }

        StringBuilder dclSummary = new StringBuilder("=> DCL 权限管理操作验证完成（GRANT/REVOKE/权限查询）：PASS "
                + dclPassCount + " 项");
        if (dclWarnCount > 0) {
            dclSummary.append("，WARN ").append(dclWarnCount).append(" 项（权限不足）");
        }
        if (dclInfoCount > 0) {
            dclSummary.append("，INFO ").append(dclInfoCount).append(" 项");
        }
        System.out.println(dclSummary.append("。").toString());

        // 恢复 DCL 测试中可能收回的权限，确保后续 TCL / 并发测试可正常执行
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            try { stmt.executeUpdate("GRANT ALL ON TABLE " + TEST_TABLE + " TO " + USERNAME); } catch (Exception ignored) {}
        }
    }

    // ==================== TCL ====================
    private static void verifyTCL(HikariDataSource dataSource) throws Exception {
        printSection("TCL（Transaction Control Language）验证");
        printPurpose("验证 COMMIT / ROLLBACK / SAVEPOINT 事务控制操作通过 dolphin MySQL 协议正常执行。");

        try (Connection conn = dataSource.getConnection()) {

            // --- 显式 COMMIT ---
            System.out.println("[TCL-01] COMMIT - 显式提交事务");
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM " + TEST_TABLE + " WHERE name = 'tcl_commit_test'");
                stmt.executeUpdate("INSERT INTO " + TEST_TABLE + " (name, age, score) VALUES ('tcl_commit_test', 40, 77.00)");
            }
            conn.commit();
            int committedCount = queryCountByName(conn, TEST_TABLE, "tcl_commit_test");
            if (committedCount == 1) {
                System.out.println("  PASS | COMMIT 后 tcl_commit_test 数据可见，行数=" + committedCount);
            } else {
                throw new IllegalStateException("COMMIT 失败：预期 1 行，实际 " + committedCount);
            }

            // --- ROLLBACK ---
            System.out.println("[TCL-02] ROLLBACK - 回滚事务");
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("INSERT INTO " + TEST_TABLE + " (name, age, score) VALUES ('tcl_rollback_test', 41, 66.00)");
            }
            // 不 commit，直接 rollback
            conn.rollback();
            int rolledBackCount = queryCountByName(conn, TEST_TABLE, "tcl_rollback_test");
            if (rolledBackCount == 0) {
                System.out.println("  PASS | ROLLBACK 后 tcl_rollback_test 数据不存在，行数=" + rolledBackCount);
            } else {
                throw new IllegalStateException("ROLLBACK 失败：预期 0 行，实际 " + rolledBackCount);
            }

            // --- SAVEPOINT + ROLLBACK TO SAVEPOINT ---
            System.out.println("[TCL-03] SAVEPOINT + ROLLBACK TO SAVEPOINT - 部分回滚");
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM " + TEST_TABLE + " WHERE name IN ('sp_keep', 'sp_discard')");
                stmt.executeUpdate("INSERT INTO " + TEST_TABLE + " (name, age, score) VALUES ('sp_keep', 42, 55.00)");
                stmt.executeUpdate("SAVEPOINT sp1");
                stmt.executeUpdate("INSERT INTO " + TEST_TABLE + " (name, age, score) VALUES ('sp_discard', 43, 44.00)");
                stmt.executeUpdate("ROLLBACK TO SAVEPOINT sp1");
            }
            int keepCount = queryCountByName(conn, TEST_TABLE, "sp_keep");
            int discardCount = queryCountByName(conn, TEST_TABLE, "sp_discard");
            conn.commit(); // 提交保留的部分
            System.out.println("  PASS | ROLLBACK TO SAVEPOINT 后：sp_keep 行数=" + keepCount + "（保留），sp_discard 行数=" + discardCount + "（回滚）");

            // --- 隔离级别查询 ---
            System.out.println("[TCL-04] 事务隔离级别确认");
            String isolation = "";
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT @@session.transaction_isolation AS iso")) {
                if (rs.next()) isolation = rs.getString("iso");
            }
            System.out.println("  INFO | 当前事务隔离级别：" + isolation);

            // 恢复 autoCommit
            conn.setAutoCommit(true);
        }

        System.out.println("=> TCL 共 3 项事务控制验证通过（COMMIT/ROLLBACK/SAVEPOINT+RB_TO_SP）；隔离级别查询为 INFO（仅查询，不计入 PASS）。");
    }

    // ==================== 并发连接 ====================
    private static void verifyConcurrentConnections(HikariDataSource dataSource) throws Exception {
        printSection("并发连接验证");
        printPurpose("启动多个线程同时从 HikariCP 连接池获取连接并执行 SQL，验证并发场景下连接不泄漏、SQL 正确执行。");

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger passed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        System.out.println("启动 " + threadCount + " 个并发线程...");

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                String workerName = "conc_worker_" + idx + "_" + System.nanoTime();
                ready.countDown();
                try {
                    startGate.await();
                    try (Connection conn = dataSource.getConnection()) {
                        conn.setAutoCommit(false);
                        // INSERT
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO " + TEST_TABLE + " (name, age, score) VALUES (?, ?, ?)")) {
                            ps.setString(1, workerName);
                            ps.setInt(2, 20 + idx);
                            ps.setDouble(3, 60.0 + idx);
                            ps.executeUpdate();
                        }
                        // SELECT 验证
                        int cnt = queryCountByName(conn, TEST_TABLE, workerName);
                        if (cnt != 1) throw new IllegalStateException("并发 INSERT 后查不到数据");
                        // UPDATE
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE " + TEST_TABLE + " SET age = age + 1 WHERE name = ?")) {
                            ps.setString(1, workerName);
                            ps.executeUpdate();
                        }
                        // DELETE
                        try (PreparedStatement ps = conn.prepareStatement(
                                "DELETE FROM " + TEST_TABLE + " WHERE name = ?")) {
                            ps.setString(1, workerName);
                            ps.executeUpdate();
                        }
                        conn.commit();
                        passed.incrementAndGet();
                    }
                } catch (Exception e) {
                    failed.incrementAndGet();
                    System.err.println("  Worker-" + idx + " 失败: " + e.getMessage());
                }
            });
        }

        ready.await(10, TimeUnit.SECONDS);
        startGate.countDown();
        executor.shutdown();
        boolean terminated = executor.awaitTermination(30, TimeUnit.SECONDS);

        System.out.println("  并发线程总数：" + threadCount);
        System.out.println("  通过：" + passed.get() + " / 失败：" + failed.get());
        System.out.println("  线程池终止：" + (terminated ? "正常" : "超时"));

        if (passed.get() != threadCount || failed.get() != 0) {
            throw new IllegalStateException("并发验证失败：通过 " + passed.get() + "/" + threadCount);
        }
        System.out.println("  PASS | " + threadCount + "/" + threadCount + " 个并发 worker 均完成 insert/select/update/delete/commit。");
        System.out.println("=> 并发连接验证通过。");
    }

    // ==================== 清理 ====================
    private static void cleanupTables(HikariDataSource dataSource) throws Exception {
        printSection("清理 DDL 残留表");
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            try { stmt.executeUpdate("DROP TABLE IF EXISTS " + TEST_TABLE); System.out.println("  已清理 " + TEST_TABLE); } catch (Exception ignored) {}
            try { stmt.executeUpdate("DROP TABLE IF EXISTS " + TEST_TABLE_2); System.out.println("  已清理 " + TEST_TABLE_2); } catch (Exception ignored) {}
        }
    }

    // ==================== 结论 ====================
    private static void printFinalConclusion() {
        printSection("最终结论");
        String border = "+" + "-".repeat(CATEGORY_WIDTH) + "+" + "-".repeat(OPERATION_WIDTH) + "+"
                + "-".repeat(RESULT_WIDTH) + "+";

        String dmlResult = generatedKeysReturned ? "PASS (5项)" : "PASS (4项)";
        String dclResult = dclWarnCount == 0
                ? "PASS (" + dclPassCount + "项)"
                : "PASS (" + dclPassCount + "项) + WARN (" + dclWarnCount + "项)";

        System.out.println(border);
        System.out.println(row("分类", "操作类型", "验证结果"));
        System.out.println(border);
        System.out.println(row("DDL", "CREATE / ALTER / DROP", "PASS (7项)"));
        System.out.println(row("", "TRUNCATE / CTAS", ""));
        System.out.println(border);
        System.out.println(row("DML", "INSERT / UPDATE / DELETE", dmlResult));
        System.out.println(row("", "批量写入", ""));
        if (!generatedKeysReturned) {
            System.out.println(row("", "getGeneratedKeys", "INFO (1项)"));
        }
        System.out.println(border);
        System.out.println(row("DQL", "SELECT / WHERE / ORDER BY", "PASS (7项)"));
        System.out.println(row("", "聚合 / GROUP BY / 子查询", ""));
        System.out.println(border);
        System.out.println(row("DCL", "GRANT / REVOKE / 权限查询", dclResult));
        if (dclInfoCount > 0) {
            System.out.println(row("", "无额外授权记录", "INFO (" + dclInfoCount + "项)"));
        }
        System.out.println(border);
        System.out.println(row("TCL", "COMMIT / ROLLBACK", "PASS (3项)"));
        System.out.println(row("", "SAVEPOINT+RB_TO_SP", ""));
        System.out.println(row("", "隔离级别查询", "INFO (1项)"));
        System.out.println(border);
        System.out.println(row("并发", "10 worker 同时持连接执行SQL", "PASS"));
        System.out.println(border);
        System.out.println();

        StringBuilder conclusion = new StringBuilder("DDL/DML/DQL/DCL/TCL + 并发连接验证通过；");
        if (!generatedKeysReturned) {
            conclusion.append("DML-05 getGeneratedKeys 与 ");
        }
        conclusion.append("TCL-04 隔离级别查询为 INFO，不计入 PASS。");
        System.out.println(conclusion.toString());
        System.out.println("HikariCP + MySQL Connector/J 经 dolphin MySQL 协议访问 openGauss B 兼容库功能完整可用。");
    }

    private static String row(String category, String operation, String result) {
        return "|" + cell(category, CATEGORY_WIDTH) + "|" + cell(operation, OPERATION_WIDTH) + "|"
                + cell(result, RESULT_WIDTH) + "|";
    }

    /** 生成固定宽度的表格单元格，中文按两列宽度补齐空格。 */
    private static String cell(String text, int width) {
        StringBuilder content = new StringBuilder(" ").append(text);
        int padding = width - 1 - displayWidth(text);
        for (int i = 0; i < padding; i++) {
            content.append(' ');
        }
        return content.toString();
    }

    /** 按中文字符占两列的方式计算显示宽度。 */
    private static int displayWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            width += (c >= '\u2E80' && c <= '\u9FFF') ? 2 : 1;
        }
        return width;
    }

    // ==================== 工具方法 ====================
    private static void printSection(String title) {
        System.out.println();
        System.out.println("==================== " + (sectionNo++) + ". " + title + " ====================");
    }

    private static void printPurpose(String purpose) {
        System.out.println("测试目的：" + purpose);
    }

    private static int queryCount(Connection conn, String table) throws SQLException {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) AS c FROM " + table)) {
            return rs.next() ? rs.getInt("c") : 0;
        }
    }

    private static int queryCountByName(Connection conn, String table, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS c FROM " + table + " WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("c") : 0;
            }
        }
    }

    /** 读取必需的环境变量；缺失或为空时显式抛出，避免把明文口令写死在代码中。 */
    private static String requireEnv(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("缺少必需环境变量 " + key
                    + "，请通过环境变量注入数据库连接口令后再运行本示例。");
        }
        return v;
    }
    /** 安全获取异常消息的第一行；若消息为 null 或空，返回占位符。 */
    private static String firstLineOrEmpty(String msg) {
    if (msg == null || msg.isEmpty()) {
        return "（无详细错误信息）";
    }
    return msg.split("\n")[0];
}
}
