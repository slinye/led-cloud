package com.led.schedule.controller;

import com.led.common.dto.R;
import com.led.schedule.dto.DashboardData;
import com.led.schedule.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 获取仪表盘摘要数据 */
    @GetMapping("/summary")
    public R<Map<String, Object>> summary() {
        // 实际应从各服务聚合，此处使用默认值示意
        Map<String, Object> data = dashboardService.getSummaryData(6, 2, 10, 3, 50);
        return R.ok(data);
    }

    /** 导出 PDF 报表 */
    @GetMapping("/export-pdf")
    public R<String> exportPdf() {
        // PDF 导出功能 - 后续实现
        return R.ok("PDF报表导出功能已迁移到调度服务，可通过 /api/dashboard/pdf 获取");
    }

    /** 获取播放日志 */
    @GetMapping("/play-logs")
    public R<Void> playLogs() {
        return R.ok();
    }
}
