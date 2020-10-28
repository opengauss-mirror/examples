package com.lamdaer.opengauss.gauss.controller;


import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lamdaer.opengauss.gauss.common.Result;
import com.lamdaer.opengauss.gauss.entity.Hobby;
import com.lamdaer.opengauss.gauss.entity.vo.HobbyVo;
import com.lamdaer.opengauss.gauss.service.HobbyService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * <p>
 * 爱好二级分类 前端控制器
 * </p>
 * @author Lamdaer
 * @since 2020-10-24
 */
@Api(tags = "爱好")
@RestController
@RequestMapping("/gauss/hobby")
public class HobbyController {
    @Autowired
    private HobbyService hobbyService;
    
    @ApiOperation("添加爱好")
    @PostMapping
    public Result addHobby(@RequestBody @Valid HobbyVo hobbyVo) {
        hobbyService.addHobby(hobbyVo);
        return Result.ok();
    }
    @ApiOperation("获取爱好列表")
    @GetMapping
    public Result getHobbyList() {
        List<Hobby> hobbyList = hobbyService.getHobbyList();
        return Result.ok().data("hobbyList", hobbyList);
    }
}

