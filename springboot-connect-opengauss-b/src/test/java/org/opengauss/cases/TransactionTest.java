package org.opengauss.cases;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opengauss.smoke.SmokeApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opengauss.cases.DbUtil.*;

/**
 * 维度四：事务。
 * Spring TransactionTemplate 提交/回滚、手动 Connection 事务、保存点、隔离级别接受性。
 * 复用冒烟环境的 `user` 表，测试数据前缀 t_tx_，用例结束自行清理。
 */
@SpringBootTest(classes = SmokeApplication.class)
class TransactionTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate txTemplate;

    private int count(String prefix) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE `name` LIKE ?", Integer.class, prefix + "%");
        return n == null ? 0 : n;
    }

    private void cleanup(String prefix) {
        jdbc.update("DELETE FROM `user` WHERE `name` LIKE ?", prefix + "%");
    }

    @Test
    @DisplayName("4.1 TransactionTemplate：提交后对外可见")
    void commitVisible() {
        txTemplate.execute(status -> {
            jdbc.update("INSERT INTO `user` (`name`, `age`) VALUES (?, ?)", "t_tx_commit", 1);
            return null;
        });
        assertEquals(1, count("t_tx_commit"), "提交后应可见");
        cleanup("t_tx_commit");
    }

    @Test
    @DisplayName("4.2 TransactionTemplate：setRollbackOnly 回滚后不可见")
    void rollbackInvisible() {
        txTemplate.execute(status -> {
            jdbc.update("INSERT INTO `user` (`name`, `age`) VALUES (?, ?)", "t_tx_rollback", 1);
            status.setRollbackOnly();
            return null;
        });
        assertEquals(0, count("t_tx_rollback"), "回滚后应不可见");
        cleanup("t_tx_rollback");
    }

    @Test
    @DisplayName("4.3 手动 Connection 事务：commit / rollback")
    void manualConnectionTx() throws SQLException {
        try (Connection c = raw(null)) {
            c.setAutoCommit(false);
            try (var st = c.createStatement()) {
                st.executeUpdate("INSERT INTO `user` (`name`, `age`) VALUES ('t_tx_manual', 1)");
            }
            c.rollback();
            assertEquals(0, count("t_tx_manual"), "rollback 后不可见");

            try (var st = c.createStatement()) {
                st.executeUpdate("INSERT INTO `user` (`name`, `age`) VALUES ('t_tx_manual', 1)");
            }
            c.commit();
        }
        assertEquals(1, count("t_tx_manual"), "commit 后可见");
        cleanup("t_tx_manual");
    }

    @Test
    @DisplayName("4.4 保存点：部分回滚")
    void savepoint() throws SQLException {
        try (Connection c = raw(null)) {
            c.setAutoCommit(false);
            try (var st = c.createStatement()) {
                st.executeUpdate("INSERT INTO `user` (`name`, `age`) VALUES ('t_tx_sp_a', 1)");
            }
            Savepoint sp = c.setSavepoint("sp1");
            try (var st = c.createStatement()) {
                st.executeUpdate("INSERT INTO `user` (`name`, `age`) VALUES ('t_tx_sp_b', 1)");
            }
            c.rollback(sp);
            c.commit();
        }
        assertEquals(1, count("t_tx_sp_a"), "保存点之前的数据应保留");
        assertEquals(0, count("t_tx_sp_b"), "保存点之后的数据应被回滚");
        cleanup("t_tx_sp_");
    }

    @Test
    @DisplayName("4.5 隔离级别接受性：READ_COMMITTED / REPEATABLE_READ / SERIALIZABLE")
    void isolationLevels() throws SQLException {
        int[] levels = {
                Connection.TRANSACTION_READ_COMMITTED,
                Connection.TRANSACTION_REPEATABLE_READ,
                Connection.TRANSACTION_SERIALIZABLE
        };
        String[] names = {"READ_COMMITTED", "REPEATABLE_READ", "SERIALIZABLE"};
        for (int i = 0; i < levels.length; i++) {
            try (Connection c = raw(null)) {
                c.setTransactionIsolation(levels[i]);
                log("4.5 setTransactionIsolation(%s) 被接受，当前隔离级别 = %d", names[i], c.getTransactionIsolation());
            } catch (SQLException e) {
                log("4.5 setTransactionIsolation(%s) 报错: %s", names[i], e.getMessage());
            }
        }
    }
}
