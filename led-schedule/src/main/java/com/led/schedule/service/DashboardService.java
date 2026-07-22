package com.led.schedule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.led.common.entity.PlayLog;
import com.led.common.entity.Program;
import com.led.schedule.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/** 仪表盘数据聚合 - 全部从数据库实时查询，在线状态基于心跳时间 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ScreenMapper screenMapper;
    private final ContentMapper contentMapper;
    private final ProgramMapper programMapper;
    private final PlayLogMapper playLogMapper;

    private static final int HEARTBEAT_TIMEOUT_SECONDS = 60;

    /** 构建仪表盘摘要数据 */
    public Map<String, Object> getSummaryData() {
        long totalScreens = screenMapper.selectCount(null);
        long onlineScreens = screenMapper.countOnline(HEARTBEAT_TIMEOUT_SECONDS);
        long playingScreens = screenMapper.countPlaying();
        long offlineScreens = totalScreens - onlineScreens;

        long totalContents = contentMapper.selectCount(null);
        long totalPrograms = programMapper.selectCount(null);

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        long todayPlayLogs = playLogMapper.selectCount(
                new LambdaQueryWrapper<PlayLog>().ge(PlayLog::getCreatedAt, todayStart));

        List<Program> recentPrograms = programMapper.selectList(
                new LambdaQueryWrapper<Program>().orderByDesc(Program::getUpdatedAt).last("LIMIT 5"));

        String onlineRate = totalScreens > 0
                ? String.format("%.1f%%", onlineScreens * 100.0 / totalScreens) : "0%";

        Map<String, Object> data = new HashMap<>();
        data.put("totalScreens", totalScreens);
        data.put("onlineScreens", onlineScreens);
        data.put("offlineScreens", offlineScreens);
        data.put("playingScreens", playingScreens);
        data.put("activeScreens", playingScreens);
        data.put("totalContents", totalContents);
        data.put("totalPrograms", totalPrograms);
        data.put("totalPlayLogsToday", todayPlayLogs);
        data.put("onlineRate", onlineRate);
        data.put("recentPrograms", recentPrograms);
        data.put("screenTotal", totalScreens);
        data.put("contentTotal", totalContents);
        data.put("programTotal", totalPrograms);
        // 心跳超时阈值（前端可展示）
        data.put("heartbeatTimeout", HEARTBEAT_TIMEOUT_SECONDS);
        return data;
    }

    /** 分页获取最近播放日志 */
    public Map<String, Object> getRecentPlayLogs(int page, int size) {
        Page<PlayLog> pageParam = new Page<>(page, size);
        IPage<PlayLog> result = playLogMapper.selectPageWithDetails(pageParam, null, null, null, null, null);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return data;
    }

    /** 内容使用统计 */
    public List<Map<String, Object>> getContentUsageStats() {
        return contentMapper.usageStats();
    }

    /** 内容类型分布 */
    public List<Map<String, Object>> getContentTypeDistribution() {
        return contentMapper.typeDistribution();
    }

    /** 今日每小时播放趋势（补全0-23小时） */
    public List<Map<String, Object>> getPlayTrendToday() {
        List<Map<String, Object>> dbData = playLogMapper.countTodayByHour();
        // 构建 0-23 小时全量数据，数据库有则填充
        Map<Integer, Long> hourMap = new HashMap<>();
        for (Map<String, Object> row : dbData) {
            Object hourObj = row.get("hour");
            Object countObj = row.get("count");
            if (hourObj != null) {
                int hour = ((Number) hourObj).intValue();
                long count = countObj != null ? ((Number) countObj).longValue() : 0;
                hourMap.put(hour, count);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> item = new HashMap<>();
            item.put("hour", String.format("%02d:00", h));
            item.put("count", hourMap.getOrDefault(h, 0L));
            result.add(item);
        }
        return result;
    }

    /** 分组状态汇总 */
    public List<Map<String, Object>> getGroupStatusSummary() {
        return screenMapper.groupStatusSummary(HEARTBEAT_TIMEOUT_SECONDS);
    }
}
