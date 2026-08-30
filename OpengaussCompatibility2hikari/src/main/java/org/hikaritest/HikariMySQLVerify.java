package org.hikaritest;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * HikariCP + MySQL JDBC Driver 连接 openGauss B 兼容库（dolphin 插件）的连通性验证示例。
 *
 * <p>背景：开源之夏 2024 的兼容性测试报告（doc/测试报告.md）指出，在 openGauss 6.0.0-RC1 上，
 * HikariCP 通过 MySQL 驱动无法创建有效连接，原因是 dolphin 对 {@code SELECT @@session.transaction_isolation}
 * 返回无法映射的隔离级别 'default'。本示例在 openGauss 7.0.0-RC3 上验证：使用正确的连接参数后，
 * HikariCP + MySQL Connector/J 可以正常建立连接并完成 CRUD。</p>
 *
 * <p>运行前请修改下方连接常量，确保：
 * <ol>
 *   <li>目标库为 B 兼容库，且已 {@code CREATE EXTENSION dolphin;}</li>
 *   <li>连接用户已通过 {@code SELECT set_native_password('user','password','%');} 设置 MySQL 原生密码</li>
 *   <li>服务端已开启 MySQL 协议端口（默认 3306）</li>
 * </ol>
 * </p>
 */
public class HikariMySQLVerify {

    // ===== 连接参数：优先从环境变量读取，避免明文口令进入版本库 =====
    private static final String JDBC_URL =
            System.getenv().getOrDefault("OPENGAUSS_MYSQL_URL",
                    "jdbc:mysql://127.0.0.1:3306/mysql_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=utf8");
    private static final String USER = System.getenv().getOrDefault("OPENGAUSS_MYSQL_USER", "mysqluser");
    private static final String PASSWORD = requireEnv("OPENGAUSS_MYSQL_PASSWORD");
    // ===========================

    private static String requireEnv(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("缺少必需环境变量 " + key
                    + "，请通过环境变量注入数据库连接口令后再运行本示例。");
        }
        return v;
    }

    private static final String TEST_TABLE = "hikari_verify_demo";

    public static void main(String[] args) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        // 连接池参数（与 openGauss 服务端超时设置保持协调）
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("HikariCP-openGauss-Verify");

        int failed = 0;
        try (HikariDataSource ds = new HikariDataSource(config)) {
            System.out.println("HikariCP dataSource created OK, poolName=" + ds.getPoolName());

            // 1) 基础连通
            try (Connection conn = ds.getConnection();
                 Statement st = conn.createStatement()) {
                try (ResultSet rs = st.executeQuery("SELECT 1")) {
                    if (rs.next() && rs.getInt(1) == 1) {
                        System.out.println("SELECT 1 -> " + rs.getInt(1));
                    } else {
                        System.out.println("SELECT 1 FAILED");
                        failed++;
                    }
                }

                // 2) 版本与协议信息
                try (ResultSet rs = st.executeQuery("SELECT version()")) {
                    if (rs.next()) {
                        System.out.println("version() -> " + rs.getString(1));
                    }
                }
            }

            // 3) CRUD 流程
            try (Connection conn = ds.getConnection();
                 Statement st = conn.createStatement()) {
                st.execute("DROP TABLE IF EXISTS " + TEST_TABLE);
                st.execute("CREATE TABLE " + TEST_TABLE + " (id INT PRIMARY KEY, name VARCHAR(64))");
                st.execute("INSERT INTO " + TEST_TABLE + " VALUES (1,'hikari'),(2,'opengauss')");

                try (ResultSet rs = st.executeQuery("SELECT id, name FROM " + TEST_TABLE + " ORDER BY id")) {
                    int rows = 0;
                    while (rs.next()) {
                        System.out.println("row -> " + rs.getInt("id") + ", " + rs.getString("name"));
                        rows++;
                    }
                    if (rows != 2) {
                        System.out.println("CRUD row count unexpected: " + rows);
                        failed++;
                    }
                }

                // 4) 事务验证
                conn.setAutoCommit(false);
                st.execute("UPDATE " + TEST_TABLE + " SET name='hikari-cp' WHERE id=1");
                conn.commit();
                try (ResultSet rs = st.executeQuery("SELECT name FROM " + TEST_TABLE + " WHERE id=1")) {
                    if (rs.next() && "hikari-cp".equals(rs.getString(1))) {
                        System.out.println("transaction commit OK -> " + rs.getString(1));
                    } else {
                        System.out.println("transaction verify FAILED");
                        failed++;
                    }
                }
                conn.setAutoCommit(true);

                st.execute("DROP TABLE IF EXISTS " + TEST_TABLE);
            }

            if (failed == 0) {
                System.out.println("== HikariCP + MySQL JDBC verify PASSED ==");
                System.exit(0);
            } else {
                System.out.println("== HikariCP + MySQL JDBC verify FAILED (" + failed + ") ==");
                System.exit(1);
            }
        } catch (Exception e) {
            // 覆盖 SQLException 与 Hikari 建池阶段可能抛出的 RuntimeException（如无法建立连接）
            System.err.println("HikariCP 连接或初始化失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
}
