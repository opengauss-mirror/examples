package org.opengauss.test;

import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 事务测试：自动提交默认值、手动提交、回滚、隔离级别设置
 * 验证从 DBCP 连接池获取的连接事务能力与直连一致（透传）
 */
public class TransactionTest extends BaseDbTest {

    @Test
    public void testAutoCommitDefaultTrue() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            assertTrue("MySQL 协议下默认 autocommit=true", conn.getAutoCommit());
        }
    }

    @Test
    public void testCommitVisibleToAnotherConnection() throws Exception {
        String name = "tx_commit_" + System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO `user`(`name`,`age`) VALUES (?,?)")) {
                ps.setString(1, name);
                ps.setInt(2, 25);
                ps.executeUpdate();
            }
            conn.commit();
        }
        // 提交后，从连接池借出的另一个连接应能看到数据
        Integer id = findIdByName(name);
        if (id == null) {
            fail("commit 后其他连接应能看到新插入的数据");
        }
        deleteById(id);
    }

    @Test
    public void testRollbackNotVisible() throws Exception {
        String name = "tx_rollback_" + System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO `user`(`name`,`age`) VALUES (?,?)")) {
                ps.setString(1, name);
                ps.setInt(2, 25);
                ps.executeUpdate();
            }
            conn.rollback();
        }
        assertNull("rollback 后数据不应可见", findIdByName(name));
    }

    @Test
    public void testSetTransactionIsolation() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, conn.getTransactionIsolation());
            conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ, conn.getTransactionIsolation());
        }
    }

    @Test
    public void testTransactionOnPooledConnection() throws Exception {
        // DBCP 包装连接应完整透传事务语义
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                st.execute("SELECT 1");
            }
            conn.commit();
            conn.setAutoCommit(true);
        }
    }

    private Integer findIdByName(String name) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM `user` WHERE name=?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    private void deleteById(int id) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM `user` WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
