package com.szgx.kunpeng.basic.filter;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.CollectionUtils;
import org.apache.shiro.web.filter.authz.AuthorizationFilter;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * @author York_Luo
 * @create 2022-09-14-20:55
 */
public class CustomRolesOrAuthorizationFilter extends AuthorizationFilter {

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {
        //验证用户是否登陆，若是未登陆直接返回异常信息
        Subject subject = getSubject(request, response);
        Object principal = subject.getPrincipal();
        if (principal == null) {
            return false;
        }
        //获取当前访问路径所需要的角色集合
        String[] rolesArray = (String[]) mappedValue;
        //没有角色限制，可以直接访问
        if (rolesArray == null || rolesArray.length == 0) {
            //没有指定角色的话不需要进行验证，放行
            return true;
        }

        Set<String> roles = CollectionUtils.asSet(rolesArray);
        //当前subject是roles中的任意一个，则有权限访问
        for (String role : roles) {
            if (subject.hasRole(role)) {
                return true;
            }
        }
        return false;
    }


}



