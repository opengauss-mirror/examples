package com.szgx.kunpeng.jdbc.service;

import org.springframework.stereotype.Service;

import java.sql.SQLException;

/**
 * 慢sql
 *
 * @author ly
 * @create 2022-08-18-11:02
 */
@Service
public interface SlowSqlService {

    String checkSlowSql(String beginTime,String endTime) throws SQLException;
}
