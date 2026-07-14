package com.led.common.config;

import com.led.common.dto.R;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import com.led.common.annotation.AuditLog;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/** 审计日志切面 */
//测试提交
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    private final HttpServletRequest request;

    public AuditLogAspect(HttpServletRequest request) {
        this.request = request;
    }

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint point, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        String module = auditLog.module();
        String action = auditLog.action();
        String description = auditLog.description();

        log.info("[审计] 模块={}, 操作={}, 描述={}, IP={}, URL={}",
                module, action, description,
                getClientIp(request), request.getRequestURI());

        try {
            Object result = point.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("[审计] 完成 耗时={}ms", cost);
            return result;
        } catch (Exception e) {
            log.warn("[审计] 异常: {}", e.getMessage());
            throw e;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
