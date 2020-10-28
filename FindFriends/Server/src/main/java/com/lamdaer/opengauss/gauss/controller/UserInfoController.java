package com.lamdaer.opengauss.gauss.controller;


import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lamdaer.opengauss.gauss.common.Result;
import com.lamdaer.opengauss.gauss.entity.UserInfo;
import com.lamdaer.opengauss.gauss.entity.vo.UserInfoVo;
import com.lamdaer.opengauss.gauss.service.UserInfoService;

import io.swagger.annotations.ApiOperation;

/**
 * <p>
 * 用户信息 前端控制器
 * </p>
 * @author Lamdaer
 * @since 2020-10-24
 */
@RestController
@RequestMapping("/gauss/user_info")
public class UserInfoController {
    
    @Autowired
    private UserInfoService userInfoService;
    
    @ApiOperation("添加用户信息")
    @PostMapping
    public Result addUserInfo(@RequestBody @Valid UserInfoVo userInfoVo) {
        Long userId = userInfoService.add(userInfoVo);
        return Result.ok().data("userId", userId);
    }
    
    @GetMapping("{userId}")
    public Result getUserInfo(@PathVariable Long userId) {
        UserInfo userInfo = userInfoService.getById(userId);
        return Result.ok().data("userInfo", userInfo);
    }
}

