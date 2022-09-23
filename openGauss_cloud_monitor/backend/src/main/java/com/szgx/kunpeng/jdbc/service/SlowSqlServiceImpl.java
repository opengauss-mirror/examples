package com.szgx.kunpeng.jdbc.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.szgx.kunpeng.basic.tool.BaseQueryTool;
import com.szgx.kunpeng.basic.tool.ExecuteResult;
import com.szgx.kunpeng.basic.tool.JobDatasource;
import com.szgx.kunpeng.basic.util.JdbcUtils;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Map;

/**
 * 慢sql
 *
 * @author ly
 * @create 2022-08-18-11:02
 */
@Service
public class SlowSqlServiceImpl implements SlowSqlService{

    @Override
    public String checkSlowSql(String beginTime, String endTime) throws SQLException {


        JobDatasource jdbcDatasource = JdbcUtils.getJobDataSource();
        BaseQueryTool baseQueryTool = new BaseQueryTool(jdbcDatasource);
        ExecuteResult result = baseQueryTool.executeSelectSql(String.format("select * from dbe_perf.get_global_full_sql_by_timestamp('%s','%s')", beginTime, endTime));
        JSONArray array = new JSONArray();
        for (Map map : result.getTable()) {
            JSONObject object = new JSONObject(map);
            array.add(object);
        }

        return array.toString();
    }

}
