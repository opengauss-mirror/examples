package com.szgx.kunpeng.basic.common.exception;

import org.apache.shiro.authc.AuthenticationException;

/**
 * @author York_Luo
 * @create 2022-09-14-21:32
 */
public class BusinessException extends AuthenticationException {
    public BusinessException(String message) {
        super(message);
    }
}
