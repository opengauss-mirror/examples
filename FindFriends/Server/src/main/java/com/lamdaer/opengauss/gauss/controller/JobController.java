package com.lamdaer.opengauss.gauss.controller;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lamdaer.opengauss.gauss.common.Result;
import com.lamdaer.opengauss.gauss.entity.Job;
import com.lamdaer.opengauss.gauss.service.JobService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * <p>
 * 岗位 前端控制器
 * </p>
 * @author Lamdaer
 * @since 2020-10-24
 */
@Api(tags = "岗位")
@RestController
@RequestMapping("/gauss/job")
public class JobController {
    @Autowired
    private JobService jobService;
    
    @ApiOperation("添加岗位")
    @PostMapping
    public Result addJob(@RequestParam String name) {
        jobService.addJob(name);
        return Result.ok();
    }
    
    @ApiOperation("获取岗位列表")
    @GetMapping
    public Result getJobList() {
        List<Job> jobList = jobService.getJobList();
        return Result.ok().data("jobList", jobList);
    }
    
}

