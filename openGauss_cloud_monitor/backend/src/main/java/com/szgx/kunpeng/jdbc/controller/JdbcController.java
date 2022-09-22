package com.szgx.kunpeng.jdbc.controller;

import com.szgx.kunpeng.jdbc.entity.vo.DownloadWRDReportReqVO;
import com.szgx.kunpeng.jdbc.entity.vo.DownloadWRDReportResVO;
import com.szgx.kunpeng.jdbc.service.SlowSqlService;
import com.szgx.kunpeng.jdbc.service.SnapshotService;
import com.szgx.kunpeng.shell.service.ShellService;
import com.szgx.kunpeng.basic.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.sql.SQLException;

/**
 * @author ly
 * @create 2022-07-29-11:53
 */
@RestController
@RequestMapping("/jdbc")
@Api(description = "jdbc模块")
public class JdbcController {


    @Autowired
    SnapshotService snapshotService;

    @Autowired
    ShellService shellService;

    @Autowired
    SlowSqlService slowSqlService;
    /**
     * 获取快照列表
     * @return
     * @throws SQLException
     */
    @GetMapping(value = "/getSnapshotList")
    @ApiOperation(value = "获取所有快照",notes = "获取所有快照")
    public Result<String> getSnapshotList() throws SQLException {

        String result = snapshotService.getSnapshotList();

        return Result.OK(result);
    }

    /**
     * 立刻生成一次快照
     * @return
     * @throws SQLException
     */
    @GetMapping("/generateSnapshot")
    @ApiOperation(value = "立即生成一次快照")
    public Result generateSnapshot() throws SQLException {

        snapshotService.generateSnapshot();

        return Result.OK();
    }


    /**
     * 下载报告
     * @param reqVO
     * @return
     * @throws IOException
     */
    @PostMapping("/downloadWRDReport")
    @ApiOperation(value = "下载报告", notes = "下载报告")
    public Result downloadWRDReport(@Valid DownloadWRDReportReqVO reqVO) throws IOException {

        DownloadWRDReportResVO wrdReportResVO;
        try {
            snapshotService.outputSnapshot(reqVO.getBeginSnapId(),reqVO.getEndSnapId(),reqVO.getReportType(),reqVO.getReportScope(),reqVO.getNodeName(),reqVO.getReportName());
            wrdReportResVO = shellService.downloadWRDReport(reqVO.getReportName());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        }

        return Result.OK(wrdReportResVO);
    }

    @GetMapping("/checkSlowSql")
    @ApiOperation(value = "获取慢sql",notes = "获取慢sql")
    public Result checkSlowSql(@NotBlank(message = "开始时间不能为空") String beginTime,@NotBlank(message = "结束时间不能为空") String endTime) throws SQLException {

        return Result.OK(slowSqlService.checkSlowSql(beginTime, endTime));
    }

}
