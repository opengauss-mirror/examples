package com.szgx.kunpeng.basic.service;

import com.szgx.kunpeng.basic.entiry.SysUserInfo;
import org.springframework.stereotype.Service;

/**
 * @author York_Luo
 * @create 2022-09-14-21:26
 */
@Service
public class SysUserInfoMapper {


    public String getRole(String username) {

        if ("admin".equals(username)) {
            return "admin";
        }
        return "";
    }

    public SysUserInfo getUserByLogin(String username) {

        SysUserInfo sysUserInfo = null;
        if ("admin".equals(username)) {
            sysUserInfo = new SysUserInfo();
            sysUserInfo.setUsername("admin");
            sysUserInfo.setPassword("123456");
            return sysUserInfo;
        }


        return sysUserInfo;
    }
}
