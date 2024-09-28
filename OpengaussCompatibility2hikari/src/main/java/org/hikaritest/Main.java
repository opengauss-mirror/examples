package org.hikaritest;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException, InterruptedException {
        HikariConfig config = new HikariConfig("/hikari.properties");
        HikariDataSource hds = new HikariDataSource(config);
        Connection cn = hds.getConnection();
        Thread.sleep(20 * 60 * 1000);
    }
}