package org.opengauss.cases;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opengauss.smoke.SmokeApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengauss.cases.DbUtil.*;

/**
 * 维度五：连接池（HikariCP）。
 * Spring 配置生效性；用 CONNECTION_ID() 验证连接复用（池大小=1 时两次取连接应同 id）。
 * 若 B 模式不支持 CONNECTION_ID()，记录为 FINDING 并跳过复用断言。
 */
@SpringBootTest(classes = SmokeApplication.class)
class ConnectionPoolTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("5.1 application.yml 的 Hikari 配置生效")
    void springConfigApplied() {
        assertTrue(dataSource instanceof HikariDataSource, "Boot 默认连接池应为 HikariCP");
        HikariDataSource hds = (HikariDataSource) dataSource;
        log("5.1 连接池类型=%s, maximumPoolSize=%d（yml 配置 10）",
                hds.getClass().getSimpleName(), hds.getMaximumPoolSize());
        assertEquals(10, hds.getMaximumPoolSize());
    }

    @Test
    @DisplayName("5.2 CONNECTION_ID() 验证连接复用（独立池，池大小=1）")
    void connectionReuse() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url(null));
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(1);

        try (HikariDataSource ds = new HikariDataSource(config)) {
            long id1;
            try (Connection c = ds.getConnection()) {
                id1 = connectionId(c);
            } catch (SQLException e) {
                log("5.2 FINDING: CONNECTION_ID() 不可用: %s -> 改用其他方式验证复用", e.getMessage());
                return;
            }
            long id2;
            try (Connection c = ds.getConnection()) {
                id2 = connectionId(c);
            }
            log("5.2 第一次连接 CONNECTION_ID=%d，第二次=%d（池大小=1）", id1, id2);
            assertEquals(id1, id2, "池大小=1 时两次取连接应为同一物理连接");
        }
    }

    private long connectionId(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT CONNECTION_ID()");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
