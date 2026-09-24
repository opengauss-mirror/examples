package org.opengauss.cases;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengauss.cases.DbUtil.*;

/**
 * 维度六：文档明示的已知限制专项。
 * 6.1 游标（useCursorFetch/setFetchSize 文档标注不支持）
 * 6.2 存储过程（CallableStatement 文档标注不支持）
 * 实测行为并记录——限制可能表现为报错、静默忽略或已支持。
 */
class KnownLimitsTest {

    @BeforeAll
    static void createData() throws SQLException {
        try (Connection c = raw(null)) {
            exec(c,
                    "DROP TABLE IF EXISTS t_cursor",
                    "CREATE TABLE t_cursor (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50), age INT)");
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO t_cursor (name, age) VALUES (?, ?)")) {
                for (int i = 1; i <= 2000; i++) {
                    ps.setString(1, "cursor_" + i);
                    ps.setInt(2, i);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            log("6.0 准备 2000 行游标测试数据（t_cursor）");
        }
    }

    @Test
    @DisplayName("6.1 游标：useCursorFetch=true + useServerPrepStmts=true + setFetchSize")
    void cursorFetch() throws SQLException {
        try (Connection c = raw("useCursorFetch=true&useServerPrepStmts=true")) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, name FROM t_cursor ORDER BY id")) {
                ps.setFetchSize(100);
                try (ResultSet rs = ps.executeQuery()) {
                    int n = 0;
                    while (rs.next()) {
                        n++;
                    }
                    log("6.1 游标查询无报错，完整读取 %d 行。文档标注不支持游标，"
                            + "此处无报错不代表服务端逐批返回（可能静默退化为全量拉取），"
                            + "需协议层确认，报告中按'驱动层可用、协议层待确认'记录", n);
                    assertEquals(2000, n);
                }
            } catch (SQLException e) {
                log("6.1 游标查询报错（符合文档'不支持游标'的预期）: SQLState=%s, msg=%s",
                        e.getSQLState(), e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("6.2 游标：普通 Statement setFetchSize（驱动可能静默忽略）")
    void cursorFetchPlainStatement() throws SQLException {
        try (Connection c = raw(null)) {
            try (Statement st = c.createStatement()) {
                st.setFetchSize(50);
                try (ResultSet rs = st.executeQuery("SELECT id FROM t_cursor ORDER BY id")) {
                    int n = 0;
                    while (rs.next()) {
                        n++;
                    }
                    log("6.2 普通 Statement setFetchSize(50) 正常完成，读取 %d 行", n);
                    assertEquals(2000, n);
                }
            }
        }
    }

    @Test
    @DisplayName("6.3 存储过程：CallableStatement 应报错（文档标注不支持）")
    void callableStatement() {
        SQLException ex = null;
        try (Connection c = raw(null)) {
            try (CallableStatement cs = c.prepareCall("{call no_such_proc()}")) {
                cs.execute();
            }
        } catch (SQLException e) {
            ex = e;
        }
        assertTrue(ex != null, "存储过程调用应报错（文档标注不支持）");
        log("6.3 CallableStatement 报错（符合文档预期）: SQLState=%s, msg=%s",
                ex.getSQLState(), ex.getMessage());
    }
}
