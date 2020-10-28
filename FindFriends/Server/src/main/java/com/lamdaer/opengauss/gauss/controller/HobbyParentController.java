package com.lamdaer.opengauss.gauss.controller;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lamdaer.opengauss.gauss.common.Result;
import com.lamdaer.opengauss.gauss.entity.HobbyParent;
import com.lamdaer.opengauss.gauss.entity.vo.HobbyParentVo;
import com.lamdaer.opengauss.gauss.service.HobbyParentService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * <p>
 * 爱好一级分类 前端控制器
 * </p>
 * @author Lamdaer
 * @since 2020-10-24
 */
@Api(tags = "爱好一级分类")
@RestController
@RequestMapping("/gauss/hobby_parent")
public class HobbyParentController {
    
    @Autowired
    private HobbyParentService hobbyParentService;
    
    @ApiOperation("添加爱好一级分类")
    @PostMapping
    public Result addHobbyParent(String name) {
        hobbyParentService.addHobbyParent(name);
        return Result.ok();
    }
    
    @ApiOperation("获取爱好一级分类")
    @GetMapping
    public Result getHobbyParentList() {
        List<HobbyParent> hobbyParentList = hobbyParentService.getHobbyParentList();
        return Result.ok().data("hobbyParentList", hobbyParentList);
    }
    
    @ApiOperation("获取爱好一级分类及其子分类")
    @GetMapping("with_children")
    public Result getHobbyParentListWithChildren() {
        List<HobbyParentVo> list = hobbyParentService.getHobbyParentListWithChildren();
        return Result.ok().data("list", list);
    }
    
}

