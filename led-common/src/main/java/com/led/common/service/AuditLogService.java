package com.led.common.service;

import com.led.common.entity.AuditLog;

/** 审计日志存储接口 */
public interface AuditLogService {
    /** 保存审计日志 */
    void save(AuditLog auditLog);
}
