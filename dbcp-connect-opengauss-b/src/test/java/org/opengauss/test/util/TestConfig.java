package org.opengauss.test.util;

import java.io.InputStream;
import java.util.Properties;

/**
 * 测试配置加载工具。
 * 默认读取 classpath 下的 dbcp.properties；支持通过 -Ddbcp.xxx=yyy 系统属性覆盖
 * （例如在不同环境执行时：mvn test -Ddbcp.url=jdbc:mysql://192.168.1.10:3308/mysql_test_db?...）
 */
public final class TestConfig {

    /** 允许被系统属性覆盖的配置项 */
    private static final String[] OVERRIDABLE = {
            "driverClassName", "url", "username", "password",
            "initialSize", "maxTotal", "maxIdle", "minIdle", "maxWaitMillis"
    };

    private TestConfig() {
    }

    public static Properties load() {
        Properties props = new Properties();
        try (InputStream in = TestConfig.class.getClassLoader()
                .getResourceAsStream("dbcp.properties")) {
            if (in == null) {
                throw new IllegalStateException("classpath 下找不到 dbcp.properties");
            }
            props.load(in);
        } catch (Exception e) {
            throw new IllegalStateException("加载 dbcp.properties 失败: " + e.getMessage(), e);
        }

        for (String key : OVERRIDABLE) {
            String sys = System.getProperty("dbcp." + key);
            if (sys != null && !sys.isEmpty()) {
                props.setProperty(key, sys);
            }
        }
        return props;
    }
}
