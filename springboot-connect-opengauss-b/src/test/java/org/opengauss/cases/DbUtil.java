package org.opengauss.cases;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** 六维度用例公共工具：从环境变量取连接信息、按参数拼 JDBC URL、统一打日志。 */
public final class DbUtil {

    public static final String HOST = env("DB_HOST", "127.0.0.1");
    public static final String PORT = env("DB_PORT", "3308");
    public static final String DB = env("DB_NAME", "mysql_test_db");
    public static final String USER = env("DB_USER", "mysql_test_db");
    public static final String PASSWORD = env("DB_PASSWORD", "ChangeMe_123");

    private DbUtil() {
    }

    /** 拼 MySQL JDBC URL；extraParams 形如 "tinyInt1isBit=false&..."，null 表示不带额外参数。 */
    public static String url(String extraParams) {
        String base = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB
                + "?useSSL=false&serverTimezone=UTC&characterEncoding=utf-8";
        return extraParams == null || extraParams.isEmpty() ? base : base + "&" + extraParams;
    }

    /** 原生连接（不走 Spring 连接池），用于驱动参数行为专项。 */
    public static Connection raw(String extraParams) throws SQLException {
        return DriverManager.getConnection(url(extraParams), USER, PASSWORD);
    }

    public static Connection raw(String extraParams, String user, String password) throws SQLException {
        return DriverManager.getConnection(url(extraParams), user, password);
    }

    /** 逐条执行 DDL。 */
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
