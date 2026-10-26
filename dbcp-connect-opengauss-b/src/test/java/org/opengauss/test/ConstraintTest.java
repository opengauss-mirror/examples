package org.opengauss.test;

import org.opengauss.test.util.TestConfig;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * JDBC 连接参数和受限行为验证：
 * <ul>
 *   <li>allowMultiQueries 默认 off：分号多查询必须报含 "Multi-statement is not allowed" 的明确错误。</li>
 *   <li>useCursorFetch=true + setFetchSize(10)：仅验证该配置下查询可执行并精确返回 3 行；
 *       <b>不证明使用了服务端游标，也不证明 dolphin 支持/不支持服务端游标</b>。</li>
 *   <li>useSSL=false：验证本地未启用 SSL 的 Dolphin MySQL 协议端口可以连接。</li>
 * </ul>
 */
public class ConstraintTest extends BaseDbTest {

    /** 约束：allowMultiQueries 默认 off —— 分号分隔多查询必须报明确的 "Multi-statement is not allowed" */
    @Test
    public void testAllowMultiQueriesDefaultOff() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            try {
                st.execute("SELECT 1; SELECT 2");
                fail("allowMultiQueries 默认关闭，分号分隔多查询应报错");
            } catch (SQLException e) {
                assertTrue("应给出明确的 Multi-statement 报错，实际: " + e.getMessage(),
                        e.getMessage() != null && e.getMessage().contains("Multi-statement is not allowed"));
            }
        }
    }

    /**
     * 配置兼容性验证：useCursorFetch=true + setFetchSize(10) 下查询应可执行并精确返回 3 行。
     * 注意：这只是配置兼容性 / 查询可用性验证，<b>不证明使用了服务端游标</b>，
     * <b>也不证明 dolphin 支持或不支持服务端游标</b>。
     */
    @Test
    public void testCursorFetchConfigCompatibility() throws Exception {
        Properties p = TestConfig.load();
        String baseUrl = p.getProperty("url");
        String cursorUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?")
                + "useCursorFetch=true&defaultFetchSize=10";
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName(p.getProperty("driverClassName"));
        ds.setUrl(cursorUrl);
        ds.setUsername(p.getProperty("username"));
        ds.setPassword(p.getProperty("password"));
        ds.setMaxTotal(1);
        ds.setMaxWaitMillis(5000);
        try (Connection conn = ds.getConnection();
             Statement st = conn.createStatement()) {
            st.setFetchSize(10);
            try (ResultSet rs = st.executeQuery("SELECT id,name FROM `user` ORDER BY id LIMIT 3")) {
                int n = 0;
                while (rs.next()) {
                    n++;
                }
                assertEquals("useCursorFetch=true 配置下查询应可执行并精确返回 3 行", 3, n);
            }
        } finally {
            closeDataSourceQuietly(ds);
        }
    }

    /** 使用 useSSL=false 的 URL 可正常连接（其余用例均基于此配置） */
    @Test
    public void testUseSslFalseConnectionWorks() throws Exception {
        String url = TestConfig.load().getProperty("url");
        assertTrue("连接串应显式包含 useSSL=false（文档要求）", url.contains("useSSL=false"));
        try (Connection conn = dataSource.getConnection()) {
            assertTrue(conn.isValid(3));
        }
    }
}
