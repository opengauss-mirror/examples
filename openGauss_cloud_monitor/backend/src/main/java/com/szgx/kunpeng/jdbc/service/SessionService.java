package com.szgx.kunpeng.jdbc.service;

import org.springframework.stereotype.Service;

import java.sql.SQLException;

/**
 * session性能模块
 *
 * @author ly
 * @create 2022-09-15-16:41
 */
@Service
public interface SessionService {


    String getSessionRelation() throws SQLException;
    String getBlockSessionInfo(String beginTime,String endTime) throws SQLException;
    String getMostWaitEvent(String startTime,String endTime) throws SQLException;
    String getEventOfSessionByTime(String startTime,String endTime) throws SQLException;
    String getEventOfSqlByTime(String startTime,String endTime) throws SQLException;



}

