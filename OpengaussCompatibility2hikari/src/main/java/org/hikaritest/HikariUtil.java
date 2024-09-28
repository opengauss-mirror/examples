package org.hikaritest;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import java.sql.Connection;
import java.sql.SQLException;

public class HikariUtil {
    private static HikariConfig config = new HikariConfig("/hikari.properties");
    private static HikariDataSource hds = new HikariDataSource(config);

    /**
     * 获取数据库连接
     * @return Connection
     * @throws SQLException
     */
    public static Connection getConnection() throws SQLException {
        if (hds == null) {
            throw new IllegalAccessError("DataSource has not been initialized.");
        }
        return hds.getConnection();
    }

    /**
     * 获取DataSource
     * @return
     * @throws SQLException
     */
    public static HikariDataSource getDataSource() throws SQLException {
        if (hds == null) {
            throw new IllegalAccessError("DataSource has not been initialized.");
        }
        return hds;
    }

    /**
     * 获取HikariPoolMXBean
     * @return HikariPoolMXBean
     * @throws SQLException
     */
    public static HikariPoolMXBean getHikariPoolMXBean() throws SQLException {
        if (hds == null) {
            throw new IllegalAccessError("DataSource has not been initialized.");
        }
        return hds.getHikariPoolMXBean();
    }

    /**
     * 关闭数据源
     */
    public static void closeDataSource() {
        if (hds != null) {
            hds.close();
        }
    }
}
