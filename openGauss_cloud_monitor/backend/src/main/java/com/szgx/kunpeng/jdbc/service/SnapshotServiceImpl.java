package com.szgx.kunpeng.jdbc.service;

import com.alibaba.fastjson.JSONObject;
import com.szgx.kunpeng.basic.util.JdbcUtils;
import com.szgx.kunpeng.shell.service.ShellService;
import com.szgx.kunpeng.basic.tool.BaseQueryTool;
import com.szgx.kunpeng.basic.tool.ExecuteResult;
import com.szgx.kunpeng.basic.tool.JobDatasource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author York_Luo
 * @create 2022-08-01-11:37
 */
@Service
public class SnapshotServiceImpl implements SnapshotService {

    String dataSourceName;
    String username;
    String password;
    String url;

    @Autowired
    ShellService shellService;

    /**
     * 获取快照列表
     * @return
     * @throws SQLException
     */
    @Override
    public String getSnapshotList() throws SQLException {

        JobDatasource jdbcDatasource = JdbcUtils.getJobDataSource();
        BaseQueryTool baseQueryTool = new BaseQueryTool(jdbcDatasource);
        ExecuteResult result = baseQueryTool.executeSelectSql("select * from snapshot.snapshot");
        List<Map<String, Object>> list = result.getTable();
        String resultJson = JSONObject.toJSONString(list);

        return resultJson;
    }

    /**
     * 生成一次快照
     */
    @Override
    public void generateSnapshot() throws SQLException {


        JobDatasource jdbcDatasource = JdbcUtils.getJobDataSource();
        BaseQueryTool baseQueryTool = new BaseQueryTool(jdbcDatasource);
        baseQueryTool.executeSelectSql("select create_wdr_snapshot()");

    }

    /**
     * 输出快照到文件
     */
    @Override
    public void outputSnapshot(String beginSnapId,String endSnapId,String reportType,String reportScope,String nodeName,String reportName) throws SQLException, IOException {


        JobDatasource jdbcDatasource = JdbcUtils.getJobDataSource();
        BaseQueryTool baseQueryTool = new BaseQueryTool(jdbcDatasource);
//        baseQueryTool.executeSelectSql("select count(1) from tb_test2; ");
        baseQueryTool.executeSelectSql("\\o \\a \\t /home/omm/test2.html;");
//        shellService.executeShell(beginSnapId,endSnapId,reportType,reportScope,nodeName,reportName);
//        baseQueryTool.executeSelectSql(String.format("select generate_wdr_report(%s,%s,'%s','%s',%s)",beginSnapId,endSnapId,reportType,reportScope,nodeName));

    }



}
