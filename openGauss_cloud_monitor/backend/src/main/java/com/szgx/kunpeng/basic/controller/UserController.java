package com.szgx.kunpeng.basic.controller;

import com.szgx.kunpeng.basic.common.CommonResultStatus;
import com.szgx.kunpeng.basic.config.JwtToken;
import com.szgx.kunpeng.basic.entiry.SysUserInfo;
import com.szgx.kunpeng.basic.util.JwtUtils;
import com.szgx.kunpeng.basic.util.Result;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.sql.SQLException;

/**
 * @author York_Luo
 * @create 2022-09-14-23:21
 */
@RestController
@RequestMapping("/basic")
public class UserController {

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping(value = "/login")
    @ApiOperation(value = "登录",notes = "登录")
    public Result login(@Valid @RequestBody SysUserInfo userInfo) throws SQLException {

        if (!"admin".equals(userInfo.getUsername())) {
            return Result.OK("用户不存在");
        }
        if (!"123456".equals(userInfo.getPassword())) {
            return Result.OK("用户密码不正确");
        }
        UsernamePasswordToken token = new UsernamePasswordToken(userInfo.getUsername(),userInfo.getPassword());
//        JwtToken jwtToken = new JwtToken(jwtUtils.generateToken(userInfo.getUsername()));
        Subject subject = SecurityUtils.getSubject();
        subject.login(token);


        return Result.OK(CommonResultStatus.LOGIN_SUCCESS);
    }


}
