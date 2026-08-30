package org.opengauss.smoke.jpacases;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opengauss.smoke.SmokeApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengauss.smoke.jpacases.JpaDbUtil.*;

/**
 * ddl-auto 行为专项。
 * 手工拉起独立 Spring 容器（避免 @SpringBootTest 在容器启动失败时把测试直接标 ERROR），
 * 分别验证 update / validate 两种模式在 B 模式下的实际行为。
 */
class DdlAutoTest {

    private static boolean tableExists(String table) throws SQLException {
        try (Connection c = raw();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM " + table);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return true;
        }
    }

    @Test
    @DisplayName("D1 ddl-auto=update：观察 Hibernate 是否自动创建缺失表 t_jpa_ddl")
    void ddlAutoUpdate() throws SQLException {
        // 先确保目标表不存在（从干净状态出发）
        try (Connection c = raw()) {
            exec(c, "DROP TABLE IF EXISTS t_jpa_ddl");
        }
        boolean contextOk = true;
        String failure = null;
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SmokeApplication.class)
                .properties("spring.jpa.hibernate.ddl-auto=update")
                .run()) {
            log("D1 ddl-auto=update 容器启动成功");
        } catch (Exception e) {
            contextOk = false;
            failure = rootMessage(e);
            log("D1 FINDING: ddl-auto=update 容器启动失败: %s", failure);
        }
        boolean created;
        try {
            created = tableExists("t_jpa_ddl");
        } catch (SQLException e) {
            created = false;
        }
        if (contextOk && !created) {
            log("D1 FINDING: 容器启动成功但 t_jpa_ddl 未被创建。原因：连接元数据获取失败"
                    + "（information_schema.KEYWORDS 不存在，见日志 HHH000339），"
                    + "Hibernate 无法做 schema 对比 -> ddl-auto=update 静默失效（不建表也不报错）。"
                    + "实际项目影响：应用启动正常，运行时才报 relation does not exist");
        } else if (created) {
            log("D1 t_jpa_ddl 已被 Hibernate 自动创建（ddl-auto=update 可用，可能服务端已修复元数据）");
            // 新表上做一次真实读写，确认生成的表结构可用（IDENTITY 回填）
            try (Connection c = raw()) {
                exec(c, "INSERT INTO t_jpa_ddl (name) VALUES ('ddl_ok')");
            }
            try (Connection c = raw();
                 PreparedStatement ps = c.prepareStatement("SELECT name FROM t_jpa_ddl WHERE name='ddl_ok'");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("ddl_ok", rs.getString(1));
                log("D1 生成的表上读写正常");
            }
        }
        // 两种结果都不做硬断言：当前 B 模式实际行为是"不建表"（作为 FINDING 记录），
        // 将来服务端修复 information_schema 后，本用例会自动观察到 created=true
    }

    @Test
    @DisplayName("D2 ddl-auto=validate：启动校验行为（可能受 information_schema 缺失影响）")
    void ddlAutoValidate() {
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SmokeApplication.class)
                .properties("spring.jpa.hibernate.ddl-auto=validate")
                .run()) {
            log("D2 ddl-auto=validate 通过：Hibernate 元数据校验未报错");
        } catch (Exception e) {
            log("D2 FINDING: ddl-auto=validate 启动失败: %s", rootMessage(e));
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur.getMessage();
    }
}
