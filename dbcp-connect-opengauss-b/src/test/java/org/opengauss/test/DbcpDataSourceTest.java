package org.opengauss.test;

import org.apache.commons.dbcp2.BasicDataSource;
import org.opengauss.test.util.TestConfig;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 连接池初始化与配置相关测试：
 * 驱动加载、配置项、初始连接数、错误配置容错
 */
public class DbcpDataSourceTest extends BaseDbTest {

    @Test
    public void testPoolConfigLoaded() {
        Properties props = TestConfig.load();
        assertEquals(props.getProperty("url"), dataSource.getUrl());
        assertEquals(props.getProperty("username"), dataSource.getUsername());
        assertTrue("password 应从配置文件映射到连接池",
                props.getProperty("password").equals(dataSource.getPassword()));
        assertEquals(props.getProperty("driverClassName"), dataSource.getDriverClassName());
        assertEquals(Integer.parseInt(props.getProperty("initialSize")), dataSource.getInitialSize());
        assertEquals(Integer.parseInt(props.getProperty("maxTotal")), dataSource.getMaxTotal());
        assertEquals(Integer.parseInt(props.getProperty("maxIdle")), dataSource.getMaxIdle());
        assertEquals(Integer.parseInt(props.getProperty("minIdle")), dataSource.getMinIdle());
        assertEquals(Long.parseLong(props.getProperty("maxWaitMillis")), dataSource.getMaxWaitMillis());
        assertEquals(props.getProperty("validationQuery"), dataSource.getValidationQuery());
        assertEquals(Boolean.parseBoolean(props.getProperty("testOnBorrow")), dataSource.getTestOnBorrow());
        assertEquals(Boolean.parseBoolean(props.getProperty("testWhileIdle")), dataSource.getTestWhileIdle());
        assertEquals(Long.parseLong(props.getProperty("timeBetweenEvictionRunsMillis")),
                dataSource.getTimeBetweenEvictionRunsMillis());
        assertEquals(Long.parseLong(props.getProperty("minEvictableIdleTimeMillis")),
                dataSource.getMinEvictableIdleTimeMillis());
        assertEquals(Boolean.parseBoolean(props.getProperty("removeAbandonedOnBorrow")),
                dataSource.getRemoveAbandonedOnBorrow());
        assertEquals(Integer.parseInt(props.getProperty("removeAbandonedTimeout")),
                dataSource.getRemoveAbandonedTimeout());
    }

    @Test
    public void testDriverExplicitlyLoadable() throws Exception {
        // DBCP2 基于 JDBC4 SPI 自动加载驱动，显式 Class.forName 也应可用
        Class<?> clazz = Class.forName("com.mysql.cj.jdbc.Driver");
        assertNotNull(clazz);
    }

    @Test
    public void testPoolStartedWithInitialSize() throws Exception {
        // 预检已触发建池：initialSize=2，连接全部归还后 idle >= 2
        assertEquals(0, dataSource.getNumActive());
        assertTrue("idle 应不少于 initialSize，实际: " + dataSource.getNumIdle(),
                dataSource.getNumIdle() >= dataSource.getInitialSize());
    }

    @Test
    public void testWrongPasswordFails() {
        BasicDataSource bad = new BasicDataSource();
        applyUrl(bad);
        bad.setPassword("definitely-wrong-password");
        bad.setMaxTotal(1);
        bad.setMaxWaitMillis(3000);
        try (Connection conn = bad.getConnection()) {
            fail("密码错误时应抛出 SQLException，却成功获取连接: " + conn);
        } catch (SQLException e) {
            // 预期：认证失败
        } finally {
            try {
                bad.close();
            } catch (SQLException ignored) {
            }
        }
    }

    @Test
    public void testWrongPortFails() {
        BasicDataSource bad = new BasicDataSource();
        bad.setDriverClassName("com.mysql.cj.jdbc.Driver");
        // 故意用未监听的端口（原端口 + 1）
        String url = TestConfig.load().getProperty("url");
        bad.setUrl(url.replaceFirst(":[0-9]+/", ":1/"));
        bad.setUsername("mysql_test_db");
        bad.setPassword("whatever");
        bad.setMaxTotal(1);
        bad.setMaxWaitMillis(3000);
        try (Connection conn = bad.getConnection()) {
            fail("端口不通时应抛出 SQLException，却成功获取连接");
        } catch (SQLException e) {
            // 预期：连接被拒绝
        } finally {
            try {
                bad.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
