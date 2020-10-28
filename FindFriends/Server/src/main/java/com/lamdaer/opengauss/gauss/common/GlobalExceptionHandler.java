package com.lamdaer.opengauss.gauss.common;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;

/**
 * @author lamdaer
 * @createTime 2020/10/23
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(GaussException.class)
    @ResponseBody
    public Result customError(GaussException e) {
        e.printStackTrace();
        log.error(e.toString());
        return Result.error().message(e.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Result methodArgumentNotValid(MethodArgumentNotValidException e) {
        e.printStackTrace();
        log.error(e.toString());
        return Result.error().message(e.getBindingResult().getFieldError().getDefaultMessage());
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result exception(Exception e) {
        e.printStackTrace();
        log.error(e.toString());
        return Result.error().message(e.getMessage());
    }
}
