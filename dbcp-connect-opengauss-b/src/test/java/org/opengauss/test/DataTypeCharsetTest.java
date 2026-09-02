package org.opengauss.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 数据类型与字符集测试：中文/emoji（utf8mb4）、常用类型往返读写、NULL 处理。
 * 用例自建测试表，互不依赖，执行完自动清理。
 */
public class DataTypeCharsetTest extends BaseDbTest {

    @Before
    public void createTable() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS test_datatype");
            st.execute("CREATE TABLE test_datatype ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "c_varchar VARCHAR(100), "
                    + "c_int INT, "
                    + "c_bigint BIGINT, "
                    + "c_decimal DECIMAL(10,2), "
                    + "c_date DATE, "
                    + "c_datetime DATETIME, "
                    + "c_text TEXT, "
                    + "c_nullable VARCHAR(20))");
        }
    }

    @After
    public void dropTable() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS test_datatype");
        }
    }

    @Test
    public void testChineseAndEmojiRoundTrip() throws Exception {
        String value = "中文😀emoji测试_" + System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO test_datatype(c_varchar) VALUES (?)")) {
            ps.setString(1, value);
            ps.executeUpdate();
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT c_varchar FROM test_datatype WHERE c_varchar=?")) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue("插入的数据应能查到", rs.next());
                assertEquals("中文与 emoji 应无损读写", value, rs.getString(1));
            }
        }
    }

    @Test
    public void testCommonTypesRoundTrip() throws Exception {
        long bigValue = 9223372036854775807L;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO test_datatype(c_int,c_bigint,c_decimal,c_date,c_datetime,c_text) "
                             + "VALUES (?,?,?,?,?,?)")) {
            ps.setInt(1, 123);
            ps.setLong(2, bigValue);
            ps.setBigDecimal(3, new BigDecimal("12345.67"));
            ps.setDate(4, Date.valueOf("2026-08-06"));
            ps.setTimestamp(5, Timestamp.valueOf("2026-08-06 12:30:00"));
            ps.setString(6, "TEXT内容");
            ps.executeUpdate();
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT c_int,c_bigint,c_decimal,c_date,c_datetime,c_text FROM test_datatype WHERE c_int=123");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(123, rs.getInt(1));
            assertEquals(bigValue, rs.getLong(2));
            assertEquals(0, new BigDecimal("12345.67").compareTo(rs.getBigDecimal(3)));
            assertEquals(Date.valueOf("2026-08-06"), rs.getDate(4));
            assertNotNull("DATETIME 应正常读写", rs.getTimestamp(5));
            assertEquals("TEXT内容", rs.getString(6));
        }
    }

    @Test
    public void testNullHandling() throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO test_datatype(c_varchar, c_nullable) VALUES (?, ?)")) {
            ps.setString(1, "null_test");
            ps.setNull(2, Types.VARCHAR);
            ps.executeUpdate();
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT c_nullable FROM test_datatype WHERE c_varchar='null_test'");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            rs.getString(1);
            assertTrue("NULL 应通过 wasNull() 识别", rs.wasNull());
        }
    }
}
