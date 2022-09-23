package com.szgx.kunpeng.basic.tool;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.szgx.kunpeng.basic.util.JdbcUtils;
import com.szgx.kunpeng.basic.util.LocalCacheUtil;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhangrb
 * @date 2021/6/25
 */
public class BaseQueryTool {

    protected static final Logger logger = LoggerFactory.getLogger(BaseQueryTool.class);

    private DataSource datasource;

    private Connection connection;
    /**
     * 当前数据库名
     */
    private String currentSchema;
    private String currentDatabase;

    /**
     * 构造方法
     *
     * @param jobDatasource
     */
    public BaseQueryTool(JobDatasource jobDatasource) throws SQLException {
        if (LocalCacheUtil.get(jobDatasource.getDatasourceName()) == null) {
            getDataSource(jobDatasource);
        } else {
            this.connection = (Connection) LocalCacheUtil.get(jobDatasource.getDatasourceName());
            if (!this.connection.isValid(500)) {
                LocalCacheUtil.remove(jobDatasource.getDatasourceName());
                getDataSource(jobDatasource);
            }
        }
//        currentSchema = getSchema(jobDatasource.getJdbcUsername());
//        currentDatabase = jobDatasource.getDatasource();
        LocalCacheUtil.set(jobDatasource.getDatasourceName(), this.connection, 4 * 60 * 60 * 1000);
    }

    private void getDataSource(JobDatasource jobDatasource) throws SQLException {

        //这里默认使用 hikari 数据源
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setUsername(jobDatasource.getJdbcUsername());
        dataSource.setPassword(jobDatasource.getJdbcPassword());
        dataSource.setJdbcUrl(jobDatasource.getJdbcUrl());
        dataSource.setDriverClassName(jobDatasource.getJdbcDriverClass());
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(0);
        dataSource.setConnectionTimeout(30000);
        this.datasource = dataSource;
        this.connection = this.datasource.getConnection();
    }

    //根据connection获取schema
    private String getSchema(String jdbcUsername) {
        String res = null;
        try {
            res = connection.getCatalog();
        } catch (SQLException e) {
            try {
                res = connection.getSchema();
            } catch (SQLException e1) {
                logger.error("[SQLException getSchema Exception] --> "
                        + "the exception message is:" + e1.getMessage());
            }
            logger.error("[getSchema Exception] --> "
                    + "the exception message is:" + e.getMessage());
        }
        // 如果res是null，则将用户名当作 schema
        if (StrUtil.isBlank(res) && StringUtils.isNotEmpty(jdbcUsername)) {
            res = jdbcUsername.toUpperCase();
        }
        return res;
    }

    //执行executeUpdate （insert,delete,update）
    public ExecuteResult executeUpdateSql(String sql){

        Long startTime = System.currentTimeMillis();
        ExecuteResult executeResult = new ExecuteResult();
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate(sql);
            Long endTime = System.currentTimeMillis();
            Double time = Double.valueOf(endTime - startTime) / 1000;
            executeResult.setCode(sql);
            executeResult.setType("UPDATE");
            executeResult.setResult("OK");
            executeResult.setTime(time+"秒");
            return executeResult;
        } catch (SQLException e) {
            executeResult.setCode(sql);
            executeResult.setSuccess(false);
            executeResult.setResult(e.getMessage());
            executeResult.setType("UPDATE");
            executeResult.setTime("0秒");
            return executeResult;
        } finally {
            JdbcUtils.close(stmt);
        }
    }

    public ExecuteResult executeSelectSql(String querySql) {

        Long startTime = System.currentTimeMillis();
        ExecuteResult executeResult = new ExecuteResult();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery(querySql);
            List list = new ArrayList();
            ResultSetMetaData md = rs.getMetaData();//获取键名
            int columnCount = md.getColumnCount();//获取行的数量
            int resultDataCount = 0;

            while (rs.next()) {
                resultDataCount += 1;
                Map<String,Object> rowData = new LinkedHashMap<>();//声明Map
                for (int i = 1; i <= columnCount; i++) {
                    rowData.put(md.getColumnName(i), rs.getObject(i));//获取键名及值
                }
                list.add(rowData);
            }

            if (resultDataCount == 0) {
                Map<String,Object> rowData = new LinkedHashMap<>();//声明Map
                for (int i = 1; i <= columnCount; i++) {
                    rowData.put(md.getColumnName(i), "");//获取键名及值
                }
                list.add(rowData);
            }

            Long endTime = System.currentTimeMillis();
            Double time = Double.valueOf(endTime - startTime) / 1000;
            executeResult.setCode(querySql);
            executeResult.setResult("OK");
            executeResult.setType("SELECT");
            executeResult.setTable(list);
            executeResult.setTime(time +"秒");
            return executeResult;
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error("[getPageDataSql Exception] --> "
                    + "the exception message is:" + e.getMessage());
            executeResult.setCode(querySql);
            executeResult.setSuccess(false);
            executeResult.setResult(e.getMessage());
            executeResult.setType("SELECT");
            executeResult.setTime("0秒");
            return executeResult;
        } finally {
            JdbcUtils.close(rs);
            JdbcUtils.close(stmt);
        }
    }
}
