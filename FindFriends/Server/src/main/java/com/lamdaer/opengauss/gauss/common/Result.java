package com.lamdaer.opengauss.gauss.common;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * @author lamdaer
 * @createTime 2020/10/23
 */
@Data
public class Result {
    private Boolean success;
    private Integer code;
    private String message;
    private Map<String, Object> data = new HashMap<>();
    
    private Result() {
    }
    
    public static Result ok() {
        Result result = new Result();
        result.setSuccess(true);
        result.setCode(ResultCode.SUCCESS);
        result.setMessage("成功");
        return result;
    }
    
    public static Result error() {
        Result result = new Result();
        result.setSuccess(false);
        result.setCode(ResultCode.ERROR);
        result.setMessage("失败");
        return result;
    }
    
    public Result success(Boolean success) {
        this.setSuccess(success);
        return this;
    }
    
    public Result code(Integer code) {
        this.setCode(code);
        return this;
    }
    
    public Result message(String message) {
        this.setMessage(message);
        return this;
    }
    
    public Result data(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
}

