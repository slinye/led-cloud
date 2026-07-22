package com.led.schedule.controller;

import com.led.common.dto.R;
import com.led.schedule.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 仪表盘摘要（兼容 /dashboard 和 /dashboard/summary） */
    @GetMapping({ "", "/summary" })
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<Map<String, Object>> summary() {
        return R.ok(dashboardService.getSummaryData());
    }

    /** 最近播放日志 */
    @GetMapping("/play-logs")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<Map<String, Object>> playLogs(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return R.ok(dashboardService.getRecentPlayLogs(page, size));
    }

    /** 内容使用统计 */
    @GetMapping("/content-usage")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<?> contentUsage() {
        return R.ok(dashboardService.getContentUsageStats());
    }

    /** 内容类型分布 */
    @GetMapping("/content-type-distribution")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<?> contentTypeDistribution() {
        return R.ok(dashboardService.getContentTypeDistribution());
    }

    /** 今日每小时播放趋势 */
    @GetMapping("/play-trend-today")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<?> playTrendToday() {
        return R.ok(dashboardService.getPlayTrendToday());
    }

    /** 分组状态汇总 */
    @GetMapping("/group-status")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public R<?> groupStatus() {
        return R.ok(dashboardService.getGroupStatusSummary());
    }
}
