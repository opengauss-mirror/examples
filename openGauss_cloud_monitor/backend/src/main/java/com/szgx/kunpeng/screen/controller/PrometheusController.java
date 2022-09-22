package com.szgx.kunpeng.screen.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.szgx.kunpeng.screen.service.PrometheusService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhangrb
 * @date 2022/8/11 10:32
 */
@RestController
@RequestMapping("/screen")
public class PrometheusController {

    @Autowired
    private PrometheusService prometheusService;

    /**
     * 获取系统指标数据
     */
    @GetMapping("/system")
    @ApiOperationSupport(order = 1)
    @ApiOperation(value = "获取系统指标数据", notes = "")
    public Object system(Integer type, String startTime, String step) {

        return prometheusService.systemData(type,startTime,step);
    }

    /**
     * 获取高斯指标数据
     */
    @GetMapping("/gauss")
    @ApiOperationSupport(order = 1)
    @ApiOperation(value = "获取高斯指标数据", notes = "")
    public Object gauss(Integer type, String startTime, String step) {

        return prometheusService.gaussData(type,startTime,step);
    }
}
