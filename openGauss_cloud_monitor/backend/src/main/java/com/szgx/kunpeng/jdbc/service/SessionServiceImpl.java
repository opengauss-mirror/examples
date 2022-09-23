package com.szgx.kunpeng.jdbc.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.szgx.kunpeng.basic.tool.BaseQueryTool;
import com.szgx.kunpeng.basic.tool.ExecuteResult;
import com.szgx.kunpeng.basic.tool.JobDatasource;
import com.szgx.kunpeng.basic.util.JdbcUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Map;

/**
 * @author ly
 * @create 2022-09-16-10:48
 */
@Service
@Slf4j
public class SessionServiceImpl implements SessionService {

    /**
     * 查看session之间的阻塞关系
     *
     * @return
     * @throws SQLException
     */
    @Override
    public String getSessionRelation() throws SQLException {

        String sql = "select * from pg_thread_wait_status";

        return getSqlExecuteResult(sql);
    }

    /**
     * 采样blocking session信息
     * @return
     */
    @Override
    public String getBlockSessionInfo(String startTime,String endTime) throws SQLException {

        String sql=String.format("select * from DBE_PERF.local_active_session where start_time between '%s' and '%s'",startTime,endTime);
        return getSqlExecuteResult(sql);
    }

    /**
     * 最耗资源的wait event
     * @return
     */
    @Override
    public String getMostWaitEvent(String startTime,String endTime) throws SQLException {

        String sql = String.format("SELECT * " +
            " FROM dbe_perf.wait_events s, (" +
            " SELECT event, COUNT (*)" +
            " FROM dbe_perf.local_active_session" +
            " WHERE sample_time between '%s' and '%s'"+
            " GROUP BY event)t WHERE s.event = t.event ORDER BY count DESC",startTime,endTime);

        return getSqlExecuteResult(sql);
    }

    /**
     * 查看最近几分钟较耗资源的session把资源都花费在哪些event上
     * @return
     */
    @Override
    public String getEventOfSessionByTime(String startTime,String endTime) throws SQLException {

//        String sql = "SELECT sessionid, start_time, event, count" +
                String sql = String.format("SELECT * " +
                " FROM (" +
                " SELECT sessionid, start_time, event, COUNT(*)" +
                " FROM dbe_perf.local_active_session" +
                " WHERE sample_time between '%s' and '%s'"+
                " GROUP BY sessionid, start_time, event) as t ORDER BY SUM(t.count) OVER (PARTITION BY t. sessionid, start_time)DESC, t.event",startTime,endTime);

        return getSqlExecuteResult(sql);
    }

    /**
     * 最近几分钟比较占资源的SQL把资源都消耗在哪些event上
     * @return
     * @throws SQLException
     */
    @Override
    public String getEventOfSqlByTime(String startTime,String endTime) throws SQLException {
//        String sql = "SELECT query_id, event, count" +
                String sql = String.format("SELECT * " +
                " FROM (" +
                " SELECT query_id, event, COUNT(*)" +
                " FROM dbe_perf.local_active_session" +
                " WHERE sample_time between '%s' and '%s'"+
                " GROUP BY query_id, event) t ORDER BY SUM (t.count) OVER (PARTITION BY t.query_id ) DESC, t.event DESC",startTime,endTime);
        return getSqlExecuteResult(sql);
    }

    String getSqlExecuteResult(String sql) throws SQLException {
        JobDatasource jdbcDatasource = JdbcUtils.getJobDataSource();
        BaseQueryTool baseQueryTool = new BaseQueryTool(jdbcDatasource);
        ExecuteResult result = baseQueryTool.executeSelectSql(sql);
        JSONArray array = new JSONArray();
        if (result.getTable() != null) {
            for (Map map : result.getTable()) {
                JSONObject object = new JSONObject(map);
                array.add(object);
            }
        }

        return array.toString();
    }

}
