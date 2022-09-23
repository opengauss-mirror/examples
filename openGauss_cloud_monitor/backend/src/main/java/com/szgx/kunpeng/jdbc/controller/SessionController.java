package com.szgx.kunpeng.jdbc.controller;

import com.szgx.kunpeng.basic.util.Result;
import com.szgx.kunpeng.jdbc.service.SessionService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

/**
 * Session性能模块
 *
 * @author ly
 * @create 2022-09-16-11:06
 */
@RestController
@RequestMapping("/session")
public class SessionController {


    @Autowired
    SessionService sessionService;

    @GetMapping(value = "/getSessionRelation")
    @ApiOperation(value = "查看session之间的阻塞关系",notes = "查看session之间的阻塞关系")
    public Result<String> getSessionRelation() throws SQLException {

        String result = sessionService.getSessionRelation();

        return Result.OK(result);
    }

    @GetMapping(value = "/getBlockSessionInfo")
    @ApiOperation(value = "采样blocking session信息",notes = "采样blocking session信息")
    public Result<String> getBlockSessionInfo(String startTime,String endTime) throws SQLException {

        String result = sessionService.getBlockSessionInfo(startTime,endTime);

        return Result.OK(result);
    }

    @GetMapping(value = "/getMostWaitEvent")
    @ApiOperation(value = "最耗资源的wait event",notes = "最耗资源的wait event")
    public Result<String> getMostWaitEvent(String startTime,String endTime) throws SQLException {

        String result = sessionService.getMostWaitEvent(startTime,endTime);

        return Result.OK(result);
    }


    @GetMapping(value = "/getEventOfSessionByTime")
    @ApiOperation(value = "查看最近几分钟较耗资源的session把资源都花费在哪些event上",notes = "查看最近几分钟较耗资源的session把资源都花费在哪些event上")
    public Result<String> getEventOfSessionByTime(String startTime,String endTime) throws SQLException {

        String result = sessionService.getEventOfSessionByTime(startTime,endTime);

        return Result.OK(result);
    }

    @GetMapping(value = "/getEventOfSqlByTime")
    @ApiOperation(value = "最近几分钟比较占资源的SQL把资源都消耗在哪些event上",notes = "最近几分钟比较占资源的SQL把资源都消耗在哪些event上")
    public Result<String> getEventOfSqlByTime(String startTime,String endTime) throws SQLException {

        String result = sessionService.getEventOfSqlByTime(startTime,endTime);

        return Result.OK(result);
    }

}
