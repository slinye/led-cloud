package com.led.schedule.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.led.common.dto.R;
import com.led.common.entity.PlayLog;
import com.led.schedule.service.PlayLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final PlayLogService playLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<Page<PlayLog>> list(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<PlayLog> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(PlayLog::getAction, keyword).or()
                    .like(PlayLog::getMessage, keyword);
        }
        wrapper.orderByDesc(PlayLog::getCreatedAt);
        return R.ok(playLogService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public R<String> export() {
        return R.ok("导出功能开发中");
    }
}
