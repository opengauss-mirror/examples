/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2025-2025. All rights reserved.
 *
 * openGauss is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *
 * http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITFOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package org.opengauss.admin.plugin.aspectj;

import com.alibaba.fastjson.JSON;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.opengauss.admin.common.annotation.Log;
import org.opengauss.admin.common.core.domain.model.LoginUser;
import org.opengauss.admin.common.enums.BusinessStatus;
import org.opengauss.admin.common.enums.HttpMethod;
import org.opengauss.admin.common.utils.SecurityUtils;
import org.opengauss.admin.common.utils.ServletUtils;
import org.opengauss.admin.common.utils.StringUtils;
import org.opengauss.admin.common.utils.ip.IpUtils;
import org.opengauss.admin.framework.manager.AsyncManager;
import org.opengauss.admin.framework.manager.factory.AsyncFactory;
import org.opengauss.admin.system.domain.SysOperLog;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Aspect that captures controller invocations to assemble audit logs.
 *
 * @version 1.0.0
 * @since 2025-08-20
 */
@Slf4j
@Aspect
@Component
public class LogAspect {
    private static final int MAX_MESSAGE_LENGTH = 2000;

    /**
     * Process controller invocations that completed successfully.
     *
     * @param joinPoint     join point information
     * @param controllerLog logging annotation on the method
     * @param jsonResult    returned payload
     */
    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult) {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    /**
     * Process controller invocations that finished with an exception.
     *
     * @param joinPoint     join point information
     * @param controllerLog logging annotation on the method
     * @param e             thrown exception
     */
    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e) {
        handleLog(joinPoint, controllerLog, e, null);
    }

    /**
     * Build and dispatch an operation log entry.
     *
     * @param joinPoint     join point information
     * @param controllerLog logging annotation
     * @param error         optional exception
     * @param jsonResult    optional response payload
     */
    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception error, Object jsonResult) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            log.warn("Skipping log capture because request attributes are unavailable.");
            return;
        }
        ServletRequestAttributes sra = (ServletRequestAttributes) attributes;
        HttpServletRequest request = sra.getRequest();
        if (request == null) {
            log.warn("Skipping log capture because HttpServletRequest is unavailable.");
            return;
        }

        SysOperLog operLog = new SysOperLog();
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null) {
            operLog.setOperName(loginUser.getUsername());
        }
        operLog.setStatus(error == null ? BusinessStatus.SUCCESS.ordinal() : BusinessStatus.FAIL.ordinal());
        if (error != null) {
            operLog.setErrorMsg(StringUtils.substring(error.getMessage(), 0, MAX_MESSAGE_LENGTH));
        }

        operLog.setOperIp(IpUtils.getIpAddr(request));
        operLog.setOperUrl(request.getRequestURI());

        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        operLog.setMethod(className + "." + methodName + "()");
        operLog.setRequestMethod(request.getMethod());

        getControllerMethodDescription(joinPoint, controllerLog, operLog, jsonResult);
        RequestContextHolder.setRequestAttributes(sra, true);
        AsyncManager.me().execute(AsyncFactory.recordOper(operLog));
    }

    /**
     * Populate operation log metadata based on the annotation details.
     *
     * @param joinPoint  current join point
     * @param log        log annotation
     * @param operLog    operation log entity
     * @param jsonResult response payload
     */
    public void getControllerMethodDescription(JoinPoint joinPoint, Log log,
                                               SysOperLog operLog, Object jsonResult) {
        operLog.setBusinessType(log.businessType().ordinal());
        operLog.setTitle(log.title());
        operLog.setOperatorType(log.operatorType().ordinal());
        if (log.isSaveRequestData()) {
            setRequestValue(joinPoint, operLog);
        }
        if (log.isSaveResponseData() && StringUtils.isNotNull(jsonResult)) {
            operLog.setJsonResult(StringUtils.substring(JSON.toJSONString(jsonResult), 0, MAX_MESSAGE_LENGTH));
        }
    }

    /**
     * Capture request parameters and add them to the operation log.
     *
     * @param joinPoint current join point
     * @param operLog   operation log entity
     */
    private void setRequestValue(JoinPoint joinPoint, SysOperLog operLog) {
        String requestMethod = operLog.getRequestMethod();
        if (HttpMethod.PUT.name().equals(requestMethod) || HttpMethod.POST.name().equals(requestMethod)) {
            String params = argsListToString(Arrays.asList(joinPoint.getArgs()));
            operLog.setOperParam(StringUtils.substring(params, 0, MAX_MESSAGE_LENGTH));
        } else {
            HttpServletRequest request = ServletUtils.getRequest();
            Object attribute = request != null
                    ? request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)
                    : null;
            if (attribute instanceof Map<?, ?> paramsMap) {
                operLog.setOperParam(StringUtils.substring(paramsMap.toString(), 0, MAX_MESSAGE_LENGTH));
            }
        }
    }

    /**
     * Convert method parameters into a printable string.
     *
     * @param paramsList method arguments
     * @return formatted parameter representation
     */
    private String argsListToString(List<?> paramsList) {
        if (paramsList == null || paramsList.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Object param : paramsList) {
            if (StringUtils.isNotNull(param) && !isFilterObject(param)) {
                builder.append(JSON.toJSONString(param)).append(' ');
            }
        }
        return builder.toString().trim();
    }

    /**
     * Determine whether to filter the object.
     *
     * @param o object to evaluate
     * @return {@code true} if the object should be excluded from logging
     */
    public boolean isFilterObject(final Object o) {
        Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            return MultipartFile.class.isAssignableFrom(clazz.getComponentType());
        }
        if (o instanceof Collection<?> collection) {
            return collection.stream().anyMatch(MultipartFile.class::isInstance);
        }
        if (o instanceof Map<?, ?> map) {
            return map.values().stream().anyMatch(MultipartFile.class::isInstance);
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse
                || o instanceof BindingResult;
    }
}
