package com.led.common.config;

import com.led.common.annotation.AuditLog;
import com.led.common.service.AuditLogService;
import com.led.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/** 审计日志切面 - 记录操作日志到数据库 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    private final HttpServletRequest request;
    private final AuditLogService auditLogService;

    public AuditLogAspect(HttpServletRequest request, AuditLogService auditLogService) {
        this.request = request;
        this.auditLogService = auditLogService;
    }

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint point, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        String module = auditLog.module();
        String action = auditLog.action();
        String description = auditLog.description();

        // 从请求头获取当前用户信息
        String token = request.getHeader("Authorization");
        String username = "unknown";
        Long userId = null;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            username = JwtUtil.getUsername(token);
            if (username == null) username = "unknown";
            userId = JwtUtil.getUserId(token);
        }

        String result = "成功";
        try {
            Object returnValue = point.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("[审计] 模块={}, 操作={}, 用户={}, IP={}, 耗时={}ms, 结果={}",
                    module, action, username, getClientIp(request), cost, result);

            // 持久化到数据库
            try {
                com.led.common.entity.AuditLog entity = new com.led.common.entity.AuditLog();
                entity.setUserId(userId);
                entity.setUsername(username);
                entity.setModule(module);
                entity.setAction(action);
                entity.setDescription(description);
                entity.setIp(getClientIp(request));
                entity.setCostTime(cost);
                entity.setRequestMethod(request.getMethod());
                entity.setRequestUrl(request.getRequestURI());
                entity.setRequestParams(getRequestParams(point));
                entity.setResponseResult(result);
                auditLogService.save(entity);
            } catch (Exception e) {
                log.error("审计日志持久化失败", e);
            }

            return returnValue;
        } catch (Exception e) {
            result = "失败: " + e.getMessage();
            long cost = System.currentTimeMillis() - start;
            log.warn("[审计] 模块={}, 操作={}, 用户={}, 耗时={}ms, 结果={}",
                    module, action, username, cost, result);

            // 失败也持久化
            try {
                com.led.common.entity.AuditLog entity = new com.led.common.entity.AuditLog();
                entity.setUserId(userId);
                entity.setUsername(username);
                entity.setModule(module);
                entity.setAction(action);
                entity.setDescription(description);
                entity.setIp(getClientIp(request));
                entity.setCostTime(cost);
                entity.setRequestMethod(request.getMethod());
                entity.setRequestUrl(request.getRequestURI());
                entity.setRequestParams(getRequestParams(point));
                entity.setResponseResult(result);
                auditLogService.save(entity);
            } catch (Exception ex) {
                log.error("审计日志持久化失败", ex);
            }

            throw e;
        }
    }

    /** 获取请求参数（截断过长内容） */
    private String getRequestParams(ProceedingJoinPoint point) {
        try {
            Object[] args = point.getArgs();
            if (args == null || args.length == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof HttpServletRequest) continue;
                if (i > 0) sb.append(", ");
                String val = args[i] != null ? args[i].toString() : "null";
                // 过滤敏感字段：密码
                val = val.replaceAll("\"password\":\"[^\"]*\"", "\"password\":\"***\"");
                val = val.replaceAll("\"oldPassword\":\"[^\"]*\"", "\"oldPassword\":\"***\"");
                val = val.replaceAll("\"newPassword\":\"[^\"]*\"", "\"newPassword\":\"***\"");
                // 截断
                if (val.length() > 500) val = val.substring(0, 500) + "...";
                sb.append(val);
            }
            String params = sb.toString();
            return params.length() > 2000 ? params.substring(0, 2000) : params;
        } catch (Exception e) {
            return "";
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
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
