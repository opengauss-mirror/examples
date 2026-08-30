package org.opengauss.smoke.jpacases;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** JPA 用例公共工具：原生 JDBC 连接（建测试表用）与统一日志。 */
public final class JpaDbUtil {

    public static final String HOST = env("DB_HOST", "127.0.0.1");
    public static final String PORT = env("DB_PORT", "3308");
    public static final String DB = env("DB_NAME", "mysql_test_db");
    public static final String USER = env("DB_USER", "mysql_test_db");
    public static final String PASSWORD = env("DB_PASSWORD", "ChangeMe_123");

    private JpaDbUtil() {
    }

    public static String url() {
        return "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB
                + "?useSSL=false&serverTimezone=UTC&characterEncoding=utf-8";
    }

    /** 原生连接（不走连接池），用于建表/DDL 与元数据检查。 */
    public static Connection raw() throws SQLException {
        return DriverManager.getConnection(url(), USER, PASSWORD);
    }

    /** 拼 MySQL JDBC URL；extraParams 形如 "useServerPrepStmts=true&..."，null 表示不带额外参数。 */
    public static String url(String extraParams) {
        String base = url();
        return extraParams == null || extraParams.isEmpty() ? base : base + "&" + extraParams;
    }

    /** 原生连接（带驱动参数），用于协议命令专项。 */
    public static Connection raw(String extraParams) throws SQLException {
        return DriverManager.getConnection(url(extraParams), USER, PASSWORD);
    }

    public static void exec(Connection conn, String... sqls) throws SQLException {
        try (Statement st = conn.createStatement()) {
            for (String sql : sqls) {
                st.execute(sql);
            }
        }
    }

    /** 统一日志前缀：[CASE] 开头的行会被整理进自测报告。 */
    public static void log(String fmt, Object... args) {
        System.out.println("[CASE] " + String.format(fmt, args));
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? def : v;
    }
}
