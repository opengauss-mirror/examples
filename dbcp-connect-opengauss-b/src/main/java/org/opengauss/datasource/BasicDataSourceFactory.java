package org.opengauss.datasource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * DBCP 连接池工厂。
 * 支持两种创建方式：
 * 1. 编程式：直接调用 {@link #create(Properties)} 传入配置
 * 2. 配置式：从 classpath 下的 dbcp.properties 加载配置 {@link #create()}
 */
public class BasicDataSourceFactory {

    private static final Logger log = LoggerFactory.getLogger(BasicDataSourceFactory.class);

    private BasicDataSourceFactory() {
    }

    /**
     * 从 classpath 加载 dbcp.properties 并创建连接池
     */
    public static BasicDataSource create() {
        return create(loadProperties());
    }

    /**
     * 根据配置创建连接池
     */
    public static BasicDataSource create(Properties props) {
        BasicDataSource ds = new BasicDataSource();

        // ---- 驱动与连接 ----
        ds.setDriverClassName(props.getProperty("driverClassName"));
        ds.setUrl(props.getProperty("url"));
        ds.setUsername(props.getProperty("username"));
        ds.setPassword(props.getProperty("password"));

        // ---- 池容量参数 ----
        ds.setInitialSize(Integer.parseInt(props.getProperty("initialSize", "2")));
        ds.setMaxTotal(Integer.parseInt(props.getProperty("maxTotal", "8")));
        ds.setMaxIdle(Integer.parseInt(props.getProperty("maxIdle", "8")));
        ds.setMinIdle(Integer.parseInt(props.getProperty("minIdle", "2")));
        ds.setMaxWaitMillis(Long.parseLong(props.getProperty("maxWaitMillis", "5000")));

        // ---- 连接校验 ----
        ds.setValidationQuery(props.getProperty("validationQuery", "SELECT 1"));
        ds.setTestOnBorrow(Boolean.parseBoolean(props.getProperty("testOnBorrow", "true")));
        ds.setTestWhileIdle(Boolean.parseBoolean(props.getProperty("testWhileIdle", "true")));
        ds.setTimeBetweenEvictionRunsMillis(
                Long.parseLong(props.getProperty("timeBetweenEvictionRunsMillis", "60000")));
        ds.setMinEvictableIdleTimeMillis(
                Long.parseLong(props.getProperty("minEvictableIdleTimeMillis", "300000")));

        // ---- 防连接泄漏 ----
        ds.setRemoveAbandonedOnBorrow(Boolean.parseBoolean(props.getProperty("removeAbandonedOnBorrow", "false")));
        ds.setRemoveAbandonedTimeout(Integer.parseInt(props.getProperty("removeAbandonedTimeout", "300")));

        log.info("DBCP 连接池配置完成: url={}, initialSize={}, maxTotal={}",
                ds.getUrl(), ds.getInitialSize(), ds.getMaxTotal());
        return ds;
    }

    /**
     * 加载 dbcp.properties 配置文件
     */
    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = BasicDataSourceFactory.class.getClassLoader()
                .getResourceAsStream("dbcp.properties")) {
            if (in == null) {
                throw new IllegalStateException("找不到配置文件 dbcp.properties，请确认它在 classpath 下");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("加载 dbcp.properties 失败", e);
        }
        return props;
    }
}
