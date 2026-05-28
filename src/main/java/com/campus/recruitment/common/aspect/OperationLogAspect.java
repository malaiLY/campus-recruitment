package com.campus.recruitment.common.aspect;

import com.campus.recruitment.common.context.LoginUserContext;
import com.campus.recruitment.entity.OperationLog;
import com.campus.recruitment.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int MAX_PARAM_LENGTH = 2000;
    private static final String SUCCESS = "SUCCESS";
    private static final String FAIL = "FAIL";

    private final OperationLogMapper operationLogMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, com.campus.recruitment.common.annotation.OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();

        OperationLog logEntity = new OperationLog();
        logEntity.setModule(operationLog.module());
        logEntity.setOperation(operationLog.operation());
        logEntity.setCreateTime(LocalDateTime.now());

        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            logEntity.setRequestMethod(request.getMethod());
            logEntity.setRequestUrl(request.getRequestURI());
            logEntity.setRequestIp(getClientIp(request));
            logEntity.setRequestParam(truncate(buildRequestParam(request)));
        }

        try {
            Object result = joinPoint.proceed();
            logEntity.setResultStatus(SUCCESS);
            return result;
        } catch (Throwable e) {
            logEntity.setResultStatus(FAIL);
            logEntity.setErrorMessage(truncate(e.getMessage()));
            throw e;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            logEntity.setCostTime(costTime);
            fillUserInfo(logEntity);
            saveLog(logEntity);
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.warn("Failed to get current request", e);
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            int commaIndex = ip.indexOf(',');
            return commaIndex > 0 ? ip.substring(0, commaIndex).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (isValidIp(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }

    private String buildRequestParam(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            sb.append("?").append(queryString);
        }
        return sb.toString();
    }

    private String truncate(String str) {
        if (str == null) {
            return null;
        }
        return str.length() > MAX_PARAM_LENGTH ? str.substring(0, MAX_PARAM_LENGTH) : str;
    }

    private void fillUserInfo(OperationLog logEntity) {
        Long userId = LoginUserContext.getUserId();
        if (userId != null) {
            logEntity.setUserId(userId);
            LoginUserContext.LoginUser loginUser = LoginUserContext.get();
            if (loginUser != null) {
                logEntity.setUsername(loginUser.getUsername());
            }
        }
    }

    private void saveLog(OperationLog logEntity) {
        try {
            operationLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("Failed to save operation log", e);
        }
    }
}
