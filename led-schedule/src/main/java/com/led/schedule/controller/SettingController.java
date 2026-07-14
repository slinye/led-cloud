package com.led.schedule.controller;

import com.led.common.annotation.AuditLog;
import com.led.common.dto.R;
import com.led.schedule.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public R<Map<String, String>> get() {
        return R.ok(settingService.getAllAsMap());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "系统设置", action = "UPDATE", description = "修改系统设置")
    public R<Void> update(@RequestBody Map<String, String> body) {
        settingService.updateFromMap(body);
        return R.okMsg("保存成功");
    }
}
