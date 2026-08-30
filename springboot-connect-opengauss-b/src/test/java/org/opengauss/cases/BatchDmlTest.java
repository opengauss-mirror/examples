package org.opengauss.cases;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opengauss.cases.DbUtil.*;

/**
 * 维度三：DML 批量。
 * PreparedStatement 批量、rewriteBatchedStatements 开关对比（计时只记录不做硬断言）、
 * Spring JdbcTemplate 批量。
 */
class BatchDmlTest {

    @BeforeAll
    static void createTable() throws SQLException {
        try (Connection c = raw(null)) {
            exec(c,
                    "DROP TABLE IF EXISTS t_batch",
                    "CREATE TABLE t_batch (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50), age INT)");
        }
    }

    @Test
    @DisplayName("3.1 PreparedStatement 批量插入 1000 行")
    void preparedBatch1000() throws SQLException {
        try (Connection c = raw(null)) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO t_batch (name, age) VALUES (?, ?)")) {
                for (int i = 1; i <= 1000; i++) {
                    ps.setString(1, "batch_" + i);
                    ps.setInt(2, i);
                    ps.addBatch();
                }
                int[] counts = ps.executeBatch();
                int sum = 0;
                for (int n : counts) {
                    sum += n;
                }
                log("3.1 executeBatch 返回 %d 条计数，合计 %d（预期 1000）", counts.length, sum);
                assertEquals(1000, sum, "批量插入应有 1000 行受影响");
            }
        }
    }

    @Test
    @DisplayName("3.2 rewriteBatchedStatements 开关对比（500 行 × 2 轮计时）")
    void rewriteBatchCompare() throws SQLException {
        for (String extra : new String[]{null, "rewriteBatchedStatements=true"}) {
            long start = System.nanoTime();
            try (Connection c = raw(extra)) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO t_batch (name, age) VALUES (?, ?)")) {
                    for (int i = 1; i <= 500; i++) {
                        ps.setString(1, "rw_" + (extra == null ? "unset" : "on") + "_" + i);
                        ps.setInt(2, i);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            log("3.2 rewriteBatchedStatements=%s 插入 500 行耗时 %d ms",
                    extra == null ? "未显式设置" : "true", ms);
        }
    }

    @Test
    @DisplayName("3.3 JdbcTemplate 批量（Spring 层面）")
    void jdbcTemplateBatch() {
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(
                new org.springframework.jdbc.datasource.SingleConnectionDataSource(
                        url(null), USER, PASSWORD, true));
        int[] rows = jdbc.batchUpdate(
                "INSERT INTO t_batch (name, age) VALUES (?, ?)",
                new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, "jt_" + i);
                        ps.setInt(2, i);
                    }

                    @Override
                    public int getBatchSize() {
                        return 5;
                    }
                });
        log("3.3 JdbcTemplate batchUpdate 返回 %d 条计数", rows.length);
        assertEquals(5, rows.length);
    }
}
