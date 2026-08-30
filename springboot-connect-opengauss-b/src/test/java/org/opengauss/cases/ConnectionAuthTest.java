package org.opengauss.cases;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opengauss.smoke.SmokeApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opengauss.cases.DbUtil.*;

/**
 * 维度一：连接与认证。
 * 验证 dolphin 协议下的认证路径（native 密码）、错误凭据拒绝、
 * 端口区分（3308 协议口 vs 5432 原生口）、连接元数据返回。
 */
@SpringBootTest(classes = SmokeApplication.class)
class ConnectionAuthTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("1.1 正常连接并读取数据库产品元数据")
    void connectAndMetaData() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            log("1.1 连接成功");
            log("1.1 productName=%s", md.getDatabaseProductName());
            log("1.1 productVersion=%s", md.getDatabaseProductVersion());
            log("1.1 driver=%s %s", md.getDriverName(), md.getDriverVersion());
            // getTables 是 ORM/框架启动时的必经元数据调用；B 模式行为实测记录
            try {
                boolean hasUser = md.getTables(DB, null, "user", null).next();
                log("1.1 getTables(user) 可用，user 表存在=%s", hasUser);
            } catch (SQLException e) {
                log("1.1 FINDING: getTables 报错: %s -> 依赖 information_schema.TABLES"
                        + " 的 JDBC 元数据 API 在 B 模式下不可用", e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("1.2 错误密码应拒绝连接")
    void wrongPassword() {
        SQLException ex = assertThrows(SQLException.class,
                () -> raw(null, USER, PASSWORD + "_wrong").close());
        log("1.2 错误密码被拒绝: SQLState=%s, msg=%s", ex.getSQLState(), ex.getMessage());
    }

    @Test
    @DisplayName("1.3 错误用户名应拒绝连接")
    void wrongUser() {
        SQLException ex = assertThrows(SQLException.class,
                () -> raw(null, USER + "_no", PASSWORD).close());
        log("1.3 错误用户名被拒绝: SQLState=%s, msg=%s", ex.getSQLState(), ex.getMessage());
    }

    @Test
    @DisplayName("1.4 MySQL 驱动连 5432 原生端口应失败（协议口是 3308）")
    void wrongPort() {
        String nativeUrl = "jdbc:mysql://" + HOST + ":5432/" + DB
                + "?useSSL=false&connectTimeout=3000";
        SQLException ex = assertThrows(SQLException.class,
                () -> java.sql.DriverManager.getConnection(nativeUrl, USER, PASSWORD).close());
        log("1.4 连 5432 被拒: SQLState=%s, msg=%s", ex.getSQLState(), ex.getMessage());
    }
}
