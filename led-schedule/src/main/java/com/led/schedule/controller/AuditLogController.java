package com.led.schedule.controller;

import com.led.common.dto.R;
import com.led.schedule.service.AuditLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 审计日志查询接口 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    /** 分页查询审计日志 */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return R.ok(auditLogQueryService.pageQuery(page, size, username, module, action, startTime, endTime));
    }
}
