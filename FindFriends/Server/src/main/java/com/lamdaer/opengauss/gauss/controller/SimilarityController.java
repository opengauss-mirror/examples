package com.lamdaer.opengauss.gauss.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lamdaer.opengauss.gauss.common.Result;
import com.lamdaer.opengauss.gauss.entity.UserInfo;
import com.lamdaer.opengauss.gauss.service.SimilarityService;

import io.swagger.annotations.ApiOperation;

/**
 * @author lamdaer
 * @createTime 2020/10/24
 */
@RestController
@RequestMapping("/gauss/similarity")
public class SimilarityController {
    @Autowired
    private SimilarityService similarityService;
    
    @ApiOperation("相似度检测")
    @GetMapping("{userId}")
    public Result getSimilarUser(@PathVariable Long userId) {
        List<UserInfo> userList = similarityService.similarity(userId);
        return Result.ok().data("similarUser", userList);
    }
}
