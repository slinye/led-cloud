package com.led.schedule.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.led.common.entity.AlarmRecord;
import com.led.common.entity.PlayLog;
import com.led.common.entity.Screen;
import com.led.schedule.mapper.AlarmRecordMapper;
import com.led.schedule.mapper.PlayLogMapper;
import com.led.schedule.mapper.ScreenMapper;
import com.led.schedule.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 告警检测定时任务
 * - 每60秒检查一次离线设备
 * - 每60秒检查一次播放失败日志
 * - 告警记录写入数据库，前端通过 API 轮询获取通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmNotificationService {

    private final ScreenMapper screenMapper;
    private final PlayLogMapper playLogMapper;
    private final AlarmRecordMapper alarmRecordMapper;
    private final SettingService settingService;

    private static final int HEARTBEAT_TIMEOUT = 60; // 心跳超时60秒
    private static final int ALARM_COOLDOWN_MINUTES = 5; // 同一设备同一类型告警冷却时间

    /** 最后一次检查的时间戳，用于播放失败增量检测 */
    private LocalDateTime lastPlayLogCheck = LocalDateTime.now();

    // ==================== 离线告警检测 ====================

    /** 每60秒检查一次离线设备 */
    @Scheduled(fixedRate = 60000)
    public void checkOfflineAlarm() {
        Map<String, String> settings = settingService.getAllAsMap();
        boolean offlineEnabled = "true".equals(settings.get("offlineAlert"));
        if (!offlineEnabled) {
            return; // 告警开关关闭
        }

        try {
            // 查询所有屏幕
            List<Screen> allScreens = screenMapper.selectList(null);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime timeoutPoint = now.minusSeconds(HEARTBEAT_TIMEOUT);

            for (Screen screen : allScreens) {
                // 判断是否离线：last_heartbeat 为空或超过阈值
                boolean isOffline = screen.getLastHeartbeat() == null
                        || screen.getLastHeartbeat().isBefore(timeoutPoint);

                if (isOffline) {
                    // 检查冷却时间，避免重复告警
                    if (isInCooldown(screen.getId(), "offline")) {
                        continue;
                    }

                    // 创建离线告警记录
                    AlarmRecord record = new AlarmRecord();
                    record.setAlarmType("offline");
                    record.setScreenId(screen.getId());
                    record.setScreenName(screen.getName());
                    record.setTitle("设备离线告警");
                    String lastHb = screen.getLastHeartbeat() != null
                            ? screen.getLastHeartbeat().format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"))
                            : "从未上线";
                    record.setMessage(String.format("设备【%s】已离线，最后心跳时间: %s，超过%d秒未收到心跳",
                            screen.getName(), lastHb, HEARTBEAT_TIMEOUT));
                    record.setStatus("unread");

                    alarmRecordMapper.insert(record);
                    log.warn("[告警] 离线: {}", record.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[告警] 离线检测异常: {}", e.getMessage(), e);
        }
    }

    // ==================== 播放失败告警检测 ====================

    /** 每60秒检查播放失败日志 */
    @Scheduled(fixedRate = 60000)
    public void checkPlayFailAlarm() {
        Map<String, String> settings = settingService.getAllAsMap();
        boolean playFailEnabled = "true".equals(settings.get("playFailAlert"));
        if (!playFailEnabled) {
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();

            // 查询最近1分钟内的播放失败日志（增量检查）
            List<PlayLog> failLogs = playLogMapper.selectList(
                    new LambdaQueryWrapper<PlayLog>()
                            .and(w -> w.like(PlayLog::getAction, "error")
                                    .or().like(PlayLog::getAction, "fail"))
                            .ge(PlayLog::getCreatedAt, lastPlayLogCheck)
                            .orderByDesc(PlayLog::getCreatedAt)
            );

            lastPlayLogCheck = now;

            for (PlayLog playLog : failLogs) {
                if (playLog.getScreenId() == null) continue;

                // 检查冷却时间
                if (isInCooldown(playLog.getScreenId(), "play_fail")) {
                    continue;
                }

                AlarmRecord record = new AlarmRecord();
                record.setAlarmType("play_fail");
                record.setScreenId(playLog.getScreenId());
                record.setScreenName(playLog.getScreenName());
                record.setTitle("播放失败告警");
                record.setMessage(String.format("设备【%s】播放异常: %s (节目ID: %s)",
                        playLog.getScreenName() != null ? playLog.getScreenName() : "未知",
                        playLog.getMessage() != null ? playLog.getMessage() : "播放失败",
                        playLog.getProgramId() != null ? playLog.getProgramId() : "未知"));
                record.setStatus("unread");

                alarmRecordMapper.insert(record);
                log.warn("[告警] 播放失败: {}", record.getMessage());
            }
        } catch (Exception e) {
            log.error("[告警] 播放失败检测异常: {}", e.getMessage(), e);
        }
    }

    // ==================== 辅助方法 ====================

    /** 检查告警冷却时间，避免同一设备同一类型频繁告警 */
    private boolean isInCooldown(Long screenId, String alarmType) {
        long count = alarmRecordMapper.selectCount(
                new LambdaQueryWrapper<AlarmRecord>()
                        .eq(AlarmRecord::getScreenId, screenId)
                        .eq(AlarmRecord::getAlarmType, alarmType)
                        .ge(AlarmRecord::getCreatedAt, LocalDateTime.now().minusMinutes(ALARM_COOLDOWN_MINUTES))
        );
        return count > 0;
    }
}
