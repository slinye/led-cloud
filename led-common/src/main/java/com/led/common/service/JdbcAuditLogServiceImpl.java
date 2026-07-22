package com.led.common.service;

import com.led.common.entity.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 审计日志 JDBC 存储实现
 * 直接使用 JdbcTemplate 写入，无需 Mapper XML
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JdbcAuditLogServiceImpl implements AuditLogService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(AuditLog auditLog) {
        try {
            jdbcTemplate.update(
                "INSERT INTO t_audit_log (user_id, username, module, action, description, ip, cost_time, request_method, request_url, request_params, response_result, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                auditLog.getUserId(),
                auditLog.getUsername(),
                auditLog.getModule(),
                auditLog.getAction(),
                auditLog.getDescription(),
                auditLog.getIp(),
                auditLog.getCostTime(),
                auditLog.getRequestMethod(),
                auditLog.getRequestUrl(),
                auditLog.getRequestParams(),
                auditLog.getResponseResult()
            );
        } catch (Exception e) {
            log.error("审计日志保存失败: module={}, action={}, error={}", 
                auditLog.getModule(), auditLog.getAction(), e.getMessage());
        }
    }
}
